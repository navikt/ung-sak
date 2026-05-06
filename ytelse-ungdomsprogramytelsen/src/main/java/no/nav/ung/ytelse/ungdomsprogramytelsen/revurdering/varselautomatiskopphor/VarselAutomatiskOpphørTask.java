package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselautomatiskopphor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;

/**
 * Task som finner løpende fagsaker der kvoteMaksDato (fra ung-deltaker-opplyser) er <= 4 uker frem i tid,
 * og oppretter revurdering med årsak RE_VARSEL_AUTOMATISK_OPPHOR for å sende varsel til bruker.
 */
@ApplicationScoped
@ProsessTask(value = VarselAutomatiskOpphørTask.TASKNAME)
public class VarselAutomatiskOpphørTask implements ProsessTaskHandler {

    public static final String TASKNAME = "varselAutomatiskOpphor";
    private static final Logger log = LoggerFactory.getLogger(VarselAutomatiskOpphørTask.class);
    private static final int VARSEL_UKER_FØR_MAKSDATO = 4;

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;

    VarselAutomatiskOpphørTask() {
    }

    @Inject
    public VarselAutomatiskOpphørTask(EntityManager entityManager,
                                      BehandlingRepository behandlingRepository,
                                      EtterlysningRepository etterlysningRepository,
                                      ProsessTaskTjeneste prosessTaskTjeneste,
                                      FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                      UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.ungdomsprogramRegisterKlient = ungdomsprogramRegisterKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dagensDato = LocalDate.now();
        var fireUkerFrem = dagensDato.plusWeeks(VARSEL_UKER_FØR_MAKSDATO);

        log.info("Starter utledning av fagsaker som nærmer seg maksdato. Dato i dag: {}, sjekker maksdato <= {}", dagensDato, fireUkerFrem);

        var løpendeFagsaker = hentLøpendeFagsaker();

        log.info("Fant {} løpende fagsaker for ungdomsytelse", løpendeFagsaker.size());

        ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();

        for (Fagsak fagsak : løpendeFagsaker) {
            try {
                var revurderingTask = vurderOgOpprettTask(fagsak, dagensDato, fireUkerFrem);
                if (revurderingTask != null) {
                    taskGruppe.addNesteSekvensiell(revurderingTask);
                }
            } catch (Exception e) {
                log.warn("Feil ved vurdering av fagsak {} for automatisk opphør-varsel", fagsak.getId(), e);
            }
        }

        if (!taskGruppe.getTasks().isEmpty()) {
            log.info("Oppretter {} revurderinger for varsel om automatisk opphør", taskGruppe.getTasks().size());
            prosessTaskTjeneste.lagre(taskGruppe);
        }
    }

    private ProsessTaskData vurderOgOpprettTask(Fagsak fagsak, LocalDate dagensDato, LocalDate fireUkerFrem) {
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (sisteBehandling.isEmpty()) {
            return null;
        }

        Behandling behandling = sisteBehandling.get();

        // Sjekk om det allerede finnes en åpen behandling med denne årsaken
        var åpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsak.getId());
        boolean harAlleredeVarselBehandling = åpneBehandlinger.stream()
            .anyMatch(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR));
        if (harAlleredeVarselBehandling) {
            log.info("Fagsak {} har allerede åpen behandling med årsak RE_VARSEL_AUTOMATISK_OPPHOR, hopper over", fagsak.getId());
            return null;
        }

        // Sjekk om det finnes en eksisterende etterlysning av typen UTTALELSE_AUTOMATISK_OPPHOR
        var eksisterendeEtterlysning = etterlysningRepository.hentSisteEtterlysning(
            behandling.getId(), EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR,
            EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);
        if (eksisterendeEtterlysning.isPresent()) {
            log.info("Fagsak {} har allerede ventende etterlysning for automatisk opphør, hopper over", fagsak.getId());
            return null;
        }

        // Sjekk om det finnes ventende prosesstasker
        var prosesstaskerForFagsak = fagsakProsessTaskRepository.finnAlleForAngittSøk(fagsak.getId(), null, null, List.of(ProsessTaskStatus.KLAR, ProsessTaskStatus.VETO, ProsessTaskStatus.FEILET), true);
        if (prosesstaskerForFagsak.stream().anyMatch(task -> task.getTaskType().equals(OpprettRevurderingEllerOpprettDiffTask.TASKNAME))) {
            log.info("Fagsak {} har allerede en ventende revurdering-task, hopper over", fagsak.getId());
            return null;
        }

        // Hent maksdato fra ung-deltaker-opplyser
        var maksdato = hentMaksdatoFraRegister(fagsak);
        if (maksdato == null) {
            return null;
        }

        // Sjekk om maksdato er innenfor varselvinduet
        if (maksdato.isAfter(fireUkerFrem) || maksdato.isBefore(dagensDato)) {
            return null;
        }

        log.info("Fagsak {} har kvoteMaksDato {} fra register som er innenfor varselvinduet. Oppretter revurdering.", fagsak.getId(), maksdato);

        ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilVurderingTask.setFagsakId(fagsak.getId());
        tilVurderingTask.setProperty(PERIODER, maksdato + "/" + maksdato);
        tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR.getKode());
        return tilVurderingTask;
    }

    /**
     * Henter kvoteMaksDato fra ung-deltaker-opplyser for fagsaken.
     * Returnerer null dersom ingen maksdato finnes (f.eks. åpen periode uten beregnet maksdato).
     */
    private LocalDate hentMaksdatoFraRegister(Fagsak fagsak) {
        var registerOpplysninger = ungdomsprogramRegisterKlient.hentForAktørId(fagsak.getAktørId().getAktørId());
        return registerOpplysninger.opplysninger().stream()
            .map(DeltakerProgramOpplysningDTO::kvoteMaksDato)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private List<Fagsak> hentLøpendeFagsaker() {
        var query = entityManager.createQuery(
            "SELECT f FROM Fagsak f WHERE f.fagsakStatus = :status AND f.ytelseType = :ytelseType",
            Fagsak.class);
        query.setParameter("status", FagsakStatus.LØPENDE);
        query.setParameter("ytelseType", FagsakYtelseType.UNGDOMSYTELSE);
        return query.getResultList();
    }
}
