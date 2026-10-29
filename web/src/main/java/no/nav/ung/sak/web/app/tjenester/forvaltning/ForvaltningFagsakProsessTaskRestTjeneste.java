package no.nav.ung.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.UPDATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType.DRIFT;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskKobling;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;

/**
 * Forvaltning av koblinger mellom fagsak og prosesstask (FAGSAK_PROSESS_TASK).
 * <p>
 * Koblingene styrer rekkefølgen prosesstasker kjøres i per fagsak. Blir en prosesstask slettet uten at koblingen fjernes,
 * kan koblingen blokkere alle senere tasker på fagsaken. Disse endepunktene lar oss finne og fjerne slike koblinger uten prosesstask.
 */
@Path(ForvaltningFagsakProsessTaskRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class ForvaltningFagsakProsessTaskRestTjeneste {

    static final String BASE_PATH = "/forvaltning/fagsak-prosess-task";
    private static final String HENT_KOBLINGER_UTEN_PROSESSTASK = "/koblinger-uten-prosesstask";
    private static final String FJERN_KOBLINGER_UTEN_PROSESSTASK = "/fjern-koblinger-uten-prosesstask";
    static final String FJERN_PATH = BASE_PATH + FJERN_KOBLINGER_UTEN_PROSESSTASK;

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private static final Logger log = LoggerFactory.getLogger(ForvaltningFagsakProsessTaskRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private EntityManager entityManager;

    public ForvaltningFagsakProsessTaskRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningFagsakProsessTaskRestTjeneste(FagsakRepository fagsakRepository,
                                                    FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                    EntityManager entityManager) {
        this.fagsakRepository = fagsakRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.entityManager = entityManager;
    }

    @GET
    @Path(HENT_KOBLINGER_UTEN_PROSESSTASK)
    @Operation(description = "Henter koblinger i FAGSAK_PROSESS_TASK der prosesstasken ikke lenger finnes, for alle saker. Koblingene kan fjernes med POST " + FJERN_PATH + ".",
        summary = "Henter koblinger uten prosesstask for alle saker",
        tags = "forvaltning")
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    public List<SakMedKoblingerUtenProsessTaskDto> hentKoblingerUtenProsessTask() {
        var saker = fagsakProsessTaskRepository.finnFagsakerMedKoblingerUtenProsessTask().stream()
            .map(fagsakId -> {
                var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
                var utenProsessTask = tilKoblingerUtenProsessTask(fagsakProsessTaskRepository.hentKoblinger(fagsakId));
                return new SakMedKoblingerUtenProsessTaskDto(fagsak.getSaksnummer(), utenProsessTask);
            })
            .toList();

        log.info("Fant koblinger uten prosesstask i FAGSAK_PROSESS_TASK for {} saker: {}", saker.size(),
            saker.stream().map(s -> s.saksnummer().getVerdi()).toList());
        return saker;
    }

    @POST
    @Path(FJERN_KOBLINGER_UTEN_PROSESSTASK)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner koblinger i FAGSAK_PROSESS_TASK der prosesstasken ikke lenger finnes, for angitte prosesstasker på fagsaken,"
        + " og frigir tasker som står i VETO blokkert av dem. Avviser hele forespørselen dersom én av prosesstaskene finnes eller ikke har kobling på fagsaken.",
        summary = "Fjerner koblinger uten prosesstask",
        tags = "forvaltning")
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = UPDATE, resource = DRIFT)
    public FjernFagsakProsessTaskKoblingerResultat fjernKoblingerUtenProsessTask(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) FjernFagsakProsessTaskKoblingerRequest request) {
        Saksnummer saksnummer = request.saksnummer();
        Long fagsakId = hentFagsakId(saksnummer);
        var prosessTaskIder = new LinkedHashSet<>(request.prosessTaskIder());
        if (prosessTaskIder.size() != request.prosessTaskIder().size()) {
            throw new IllegalArgumentException("Listen med prosessTaskIder inneholder duplikater: " + request.prosessTaskIder());
        }

        Map<Long, FagsakProsessTaskKobling> koblinger = fagsakProsessTaskRepository.hentKoblinger(fagsakId).stream()
            .collect(Collectors.toMap(FagsakProsessTaskKobling::prosessTaskId, Function.identity()));
        for (Long prosessTaskId : prosessTaskIder) {
            var kobling = koblinger.get(prosessTaskId);
            if (kobling == null) {
                throw new IllegalArgumentException("Fjernet ingen koblinger for saksnummer " + saksnummer.getVerdi()
                    + ". Prosesstask " + prosessTaskId + " har ingen kobling på fagsaken.");
            }
            if (!kobling.prosessTaskMangler()) {
                throw new IllegalArgumentException("Fjernet ingen koblinger for saksnummer " + saksnummer.getVerdi()
                    + ". Prosesstask " + prosessTaskId + " finnes (status " + kobling.prosessTaskStatus() + ").");
            }
        }

        int antallTaskerFrigittFraVeto = fagsakProsessTaskRepository.fjernKoblingerUtenProsessTask(fagsakId, prosessTaskIder);

        var fjernet = prosessTaskIder.stream().map(koblinger::get).toList();
        String diagnostikkTekst = "Fjernet koblinger uten prosesstask i FAGSAK_PROSESS_TASK til prosesstasker %s. Frigitt %d tasker fra VETO. Begrunnelse: %s"
            .formatted(fjernet.stream().map(k -> k.prosessTaskId() + " (" + k.taskType() + ", gruppe_sekvensnr=" + k.gruppeSekvensNr() + ")").toList(),
                antallTaskerFrigittFraVeto, request.begrunnelse().getTekst());
        entityManager.persist(new DiagnostikkFagsakLogg(fagsakId, FJERN_PATH, diagnostikkTekst));

        log.info("Fjernet koblinger uten prosesstask i FAGSAK_PROSESS_TASK for saksnummer={} til prosesstasker {}. Frigitt {} tasker fra VETO.",
            saksnummer.getVerdi(), prosessTaskIder, antallTaskerFrigittFraVeto);

        return new FjernFagsakProsessTaskKoblingerResultat(List.copyOf(prosessTaskIder), antallTaskerFrigittFraVeto);
    }

    private Long hentFagsakId(Saksnummer saksnummer) {
        return fagsakRepository.hentSakGittSaksnummer(saksnummer)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke fagsak for saksnummer: " + saksnummer.getVerdi()))
            .getId();
    }

    private static List<KoblingUtenProsessTaskDto> tilKoblingerUtenProsessTask(List<FagsakProsessTaskKobling> koblinger) {
        TreeSet<Long> sekvensnumre = koblinger.stream()
            .map(FagsakProsessTaskKobling::gruppeSekvensNr)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(TreeSet::new));
        // Samme regel som FagsakProsessTaskRepository.sjekkTillattKjøreFagsakProsessTask: tasker med høyere gruppe_sekvensnr
        // enn laveste på fagsaken blokkeres.
        boolean finnesSenereGrupper = sekvensnumre.size() > 1;
        Long lavesteSekvensnr = sekvensnumre.isEmpty() ? null : sekvensnumre.first();

        return koblinger.stream()
            .filter(FagsakProsessTaskKobling::prosessTaskMangler)
            .map(k -> new KoblingUtenProsessTaskDto(
                k.prosessTaskId(),
                k.behandlingId(),
                k.taskType(),
                k.gruppeSekvensNr(),
                k.opprettetTid(),
                finnesSenereGrupper && Objects.equals(k.gruppeSekvensNr(), lavesteSekvensnr)))
            .toList();
    }

    public record FjernFagsakProsessTaskKoblingerRequest(
        @NotNull
        @Valid
        Saksnummer saksnummer,

        @NotNull
        @Size(min = 1, max = 10)
        List<@NotNull @Min(1) @Max(Long.MAX_VALUE) Long> prosessTaskIder,

        @NotNull
        @Valid
        KortTekst begrunnelse
    ) {
    }

    public record KoblingUtenProsessTaskDto(
        Long prosessTaskId,
        Long behandlingId,
        String taskType,
        Long gruppeSekvensNr,
        LocalDateTime opprettetTid,
        @Schema(description = "Koblingen har laveste gruppe_sekvensnr på fagsaken, og blokkerer tasker i senere grupper til prosesstasken er ferdig.")
        boolean blokkererAndreTasker
    ) {
    }

    public record SakMedKoblingerUtenProsessTaskDto(
        Saksnummer saksnummer,
        List<KoblingUtenProsessTaskDto> koblingerUtenProsessTask
    ) {
    }

    public record FjernFagsakProsessTaskKoblingerResultat(
        List<Long> fjernedeProsessTaskIder,
        int antallTaskerFrigittFraVeto
    ) {
    }
}
