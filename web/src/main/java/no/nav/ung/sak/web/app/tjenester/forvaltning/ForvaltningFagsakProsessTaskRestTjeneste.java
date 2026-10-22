package no.nav.ung.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.UPDATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType.DRIFT;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskKobling;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

/**
 * Forvaltning av koblinger mellom fagsak og prosesstask (FAGSAK_PROSESS_TASK).
 * <p>
 * Koblingene styrer rekkefølgen prosesstasker kjøres i per fagsak. Blir en prosesstask slettet uten at koblingen fjernes,
 * kan koblingen blokkere alle senere tasker på fagsaken. Disse endepunktene lar oss finne (per sak eller for alle saker) og fjerne
 * slike foreldreløse koblinger.
 */
@Path("/forvaltning/fagsak-prosess-task")
@ApplicationScoped
@Transactional
public class ForvaltningFagsakProsessTaskRestTjeneste {

    static final String HENT_PATH = "/forvaltning/fagsak-prosess-task/hent";
    static final String FJERN_PATH = "/forvaltning/fagsak-prosess-task/fjern";

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

    @POST
    @Path("/hent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter koblinger mellom fagsak og prosesstask (FAGSAK_PROSESS_TASK), også foreldreløse koblinger der prosesstasken ikke lenger finnes."
        + " Logger hvem som har hatt innsyn i saken, med begrunnelse.",
        summary = "Henter koblinger mellom fagsak og prosesstask",
        tags = "forvaltning",
        responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = FagsakProsessTaskKoblingDto.class)))))
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    public Response hentKoblinger(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) HentFagsakProsessTaskKoblingerRequest request) {
        Saksnummer saksnummer = request.saksnummer();
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isEmpty()) {
            return feil(Response.Status.NOT_FOUND, "Fant ikke fagsak for saksnummer: " + saksnummer.getVerdi());
        }
        Long fagsakId = fagsak.get().getId();
        entityManager.persist(new DiagnostikkFagsakLogg(fagsakId, HENT_PATH, request.begrunnelse().getTekst()));
        entityManager.flush();

        return Response.ok(tilDto(fagsakProsessTaskRepository.hentKoblinger(fagsakId))).build();
    }

    @GET
    @Path("/foreldrelose")
    @Operation(description = "Henter foreldreløse koblinger i FAGSAK_PROSESS_TASK (prosesstasken finnes ikke lenger) for alle saker.",
        summary = "Henter foreldreløse koblinger mellom fagsak og prosesstask for alle saker",
        tags = "forvaltning",
        responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = SakMedForeldreløseKoblingerDto.class)))))
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    public Response hentForeldreløseKoblinger() {
        var saker = fagsakProsessTaskRepository.finnFagsakerMedForeldreløseKoblinger().stream()
            .map(fagsakId -> {
                var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
                var foreldreløse = tilDto(fagsakProsessTaskRepository.hentKoblinger(fagsakId)).stream()
                    .filter(FagsakProsessTaskKoblingDto::foreldreløs)
                    .toList();
                return new SakMedForeldreløseKoblingerDto(fagsak.getSaksnummer(), foreldreløse);
            })
            .toList();

        log.info("Fant foreldreløse koblinger i FAGSAK_PROSESS_TASK for {} saker: {}", saker.size(),
            saker.stream().map(s -> s.saksnummer().getVerdi()).toList());
        return Response.ok(saker).build();
    }

    @POST
    @Path("/fjern")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner foreldreløse koblinger i FAGSAK_PROSESS_TASK (prosesstasken finnes ikke lenger) for angitte prosesstasker på fagsaken,"
        + " og frigir tasker som står i VETO blokkert av dem. Avviser hele forespørselen dersom én av prosesstaskene ikke har en foreldreløs kobling på fagsaken.",
        summary = "Fjerner foreldreløse koblinger mellom fagsak og prosesstask",
        tags = "forvaltning",
        responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = FjernFagsakProsessTaskKoblingerResultat.class))))
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = UPDATE, resource = DRIFT)
    public Response fjernForeldreløseKoblinger(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) FjernFagsakProsessTaskKoblingerRequest request) {
        Saksnummer saksnummer = request.saksnummer();
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isEmpty()) {
            return feil(Response.Status.NOT_FOUND, "Fant ikke fagsak for saksnummer: " + saksnummer.getVerdi());
        }
        var prosessTaskIder = new LinkedHashSet<>(request.prosessTaskIder());
        if (prosessTaskIder.size() != request.prosessTaskIder().size()) {
            return feil(Response.Status.BAD_REQUEST, "Listen med prosessTaskIder inneholder duplikater: " + request.prosessTaskIder());
        }

        Long fagsakId = fagsak.get().getId();
        Map<Long, FagsakProsessTaskKobling> koblinger = fagsakProsessTaskRepository.hentKoblinger(fagsakId).stream()
            .collect(Collectors.toMap(FagsakProsessTaskKobling::prosessTaskId, Function.identity()));
        var ugyldige = prosessTaskIder.stream()
            .map(id -> valider(id, koblinger.get(id)))
            .filter(Objects::nonNull)
            .toList();
        if (!ugyldige.isEmpty()) {
            return feil(Response.Status.BAD_REQUEST, "Fjernet ingen koblinger for saksnummer " + saksnummer.getVerdi() + ". " + String.join(" ", ugyldige));
        }

        int frigitt = fagsakProsessTaskRepository.fjernForeldreløseKoblinger(fagsakId, prosessTaskIder);

        var fjernet = prosessTaskIder.stream().map(koblinger::get).toList();
        String diagnostikkTekst = "Fjernet foreldreløse koblinger i FAGSAK_PROSESS_TASK til prosesstasker %s. Frigitt %d tasker fra VETO. Begrunnelse: %s"
            .formatted(fjernet.stream().map(k -> k.prosessTaskId() + " (" + k.taskType() + ", gruppe_sekvensnr=" + k.gruppeSekvensNr() + ")").toList(),
                frigitt, request.begrunnelse().getTekst());
        entityManager.persist(new DiagnostikkFagsakLogg(fagsakId, FJERN_PATH, diagnostikkTekst));
        entityManager.flush();

        log.info("Fjernet foreldreløse koblinger i FAGSAK_PROSESS_TASK for saksnummer={} til prosesstasker {}. Frigitt {} tasker fra VETO.",
            saksnummer.getVerdi(), prosessTaskIder, frigitt);

        return Response.ok(new FjernFagsakProsessTaskKoblingerResultat(List.copyOf(prosessTaskIder), frigitt)).build();
    }

    private static String valider(Long prosessTaskId, FagsakProsessTaskKobling kobling) {
        if (kobling == null) {
            return "Prosesstask " + prosessTaskId + " har ingen kobling på fagsaken.";
        }
        if (!kobling.erForeldreløs()) {
            return "Prosesstask " + prosessTaskId + " finnes (status " + kobling.prosessTaskStatus() + "), koblingen er ikke foreldreløs.";
        }
        return null;
    }

    static List<FagsakProsessTaskKoblingDto> tilDto(List<FagsakProsessTaskKobling> koblinger) {
        var sekvensnumre = koblinger.stream()
            .map(FagsakProsessTaskKobling::gruppeSekvensNr)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        // Samme regel som FagsakProsessTaskRepository.sjekkTillattKjøreFagsakProsessTask: tasker med høyere gruppe_sekvensnr
        // enn laveste på fagsaken blokkeres.
        Long lavesteSekvensnr = sekvensnumre.stream().min(Comparator.naturalOrder()).orElse(null);
        boolean finnesSenereGrupper = sekvensnumre.size() > 1;

        return koblinger.stream()
            .map(k -> new FagsakProsessTaskKoblingDto(
                k.prosessTaskId(),
                k.behandlingId(),
                k.taskType(),
                k.gruppeSekvensNr(),
                k.opprettetTid(),
                k.prosessTaskStatus(),
                k.blokkertAv(),
                k.erForeldreløs(),
                finnesSenereGrupper && Objects.equals(k.gruppeSekvensNr(), lavesteSekvensnr)))
            .toList();
    }

    private Response feil(Response.Status status, String melding) {
        log.warn(melding);
        return Response.status(status).entity(melding).build();
    }

    public record HentFagsakProsessTaskKoblingerRequest(
        @StandardAbacAttributt(StandardAbacAttributtType.SAKSNUMMER)
        @NotNull
        @Valid
        Saksnummer saksnummer,

        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        KortTekst begrunnelse
    ) {
    }

    public record FjernFagsakProsessTaskKoblingerRequest(
        @StandardAbacAttributt(StandardAbacAttributtType.SAKSNUMMER)
        @NotNull
        @Valid
        Saksnummer saksnummer,

        @NotNull
        @Size(min = 1, max = 10)
        List<@NotNull @Min(1) @Max(Long.MAX_VALUE) Long> prosessTaskIder,

        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        KortTekst begrunnelse
    ) {
    }

    public record FagsakProsessTaskKoblingDto(
        Long prosessTaskId,
        String behandlingId,
        String taskType,
        Long gruppeSekvensNr,
        LocalDateTime opprettetTid,
        ProsessTaskStatus prosessTaskStatus,
        Long blokkertAv,
        @Schema(description = "Prosesstasken finnes ikke lenger, men koblingen ligger igjen. Kan fjernes med POST " + FJERN_PATH + ".")
        boolean foreldreløs,
        @Schema(description = "Koblingen har laveste gruppe_sekvensnr på fagsaken, og blokkerer tasker i senere grupper til prosesstasken er ferdig.")
        boolean blokkererAndreTasker
    ) {
    }

    public record SakMedForeldreløseKoblingerDto(
        Saksnummer saksnummer,
        List<FagsakProsessTaskKoblingDto> foreldreløseKoblinger
    ) {
    }

    public record FjernFagsakProsessTaskKoblingerResultat(
        List<Long> fjernedeProsessTaskIder,
        int frigittFraVeto
    ) {
    }
}
