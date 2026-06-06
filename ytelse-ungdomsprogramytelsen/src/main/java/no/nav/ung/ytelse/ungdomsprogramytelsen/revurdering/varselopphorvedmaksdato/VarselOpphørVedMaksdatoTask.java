package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.MaksdatoOpphørVarslingPeriode;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;

/**
 * Task som finner løpende fagsaker der periodeMaksDato (fra ung-deltaker-opplyser) er <= 3 uker frem i tid,
 * og oppretter revurdering med årsak RE_VARSEL_OPPHOR_VED_MAKSDATO for å sende varsel til bruker.
 */
@ApplicationScoped
@ProsessTask(value = VarselOpphørVedMaksdatoTask.TASKNAME)
public class VarselOpphørVedMaksdatoTask implements ProsessTaskHandler {

    public static final String TASKNAME = "varselOpphorVedMaksdato";
    private static final Logger log = LoggerFactory.getLogger(VarselOpphørVedMaksdatoTask.class);
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private AktuelleFagsakerForMaksdatoVarselRepository aktuellleFagsakerRepository;

    VarselOpphørVedMaksdatoTask() {
    }

    @Inject
    public VarselOpphørVedMaksdatoTask(BehandlingRepository behandlingRepository,
                                       ProsessTaskTjeneste prosessTaskTjeneste,
                                       UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, AktuelleFagsakerForMaksdatoVarselRepository aktuellleFagsakerRepository) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.aktuellleFagsakerRepository = aktuellleFagsakerRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dagensDato = LocalDate.now();
        var treUkerFrem = dagensDato.plusWeeks(MaksdatoOpphørVarslingPeriode.VARSEL_UKER_FØR_MAKSDATO);

        log.info("Starter utledning av fagsaker som nærmer seg maksdato. Dato i dag: {}, sjekker maksdato <= {}", dagensDato, treUkerFrem);

        var aktuelleFagsaker = hentAktuelleFagsaker();

        log.info("Fant {} aktuelle fagsaker for ungdomsytelse", aktuelleFagsaker.size());

        ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();

        for (Fagsak fagsak : aktuelleFagsaker) {
            try {
                var revurderingTask = opprettTask(fagsak);
                if (revurderingTask != null) {
                    taskGruppe.addNesteSekvensiell(revurderingTask);
                }
            } catch (Exception e) {
                log.warn("Feil ved vurdering av fagsak {} for opphør ved maksdato-varsel", fagsak.getId(), e);
            }
        }

        if (!taskGruppe.getTasks().isEmpty()) {
            log.info("Oppretter {} revurderinger for varsel om opphør ved maksdato", taskGruppe.getTasks().size());
            prosessTaskTjeneste.lagre(taskGruppe);
        }
    }

    private ProsessTaskData opprettTask(Fagsak fagsak) {
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (sisteBehandling.isEmpty()) {
            return null;
        }

        Behandling behandling = sisteBehandling.get();
        var maksdato = ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(behandling.getId()).orElse(null);

        log.info("Fagsak {} har periodeMaksDato {} fra register som er innenfor varselvinduet. Oppretter revurdering.", fagsak.getId(), maksdato);

        var ønsketPeriode = maksdato + "/" + maksdato;
        var ønsketÅrsak = BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO.getKode();
        ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilVurderingTask.setFagsakId(fagsak.getId());
        tilVurderingTask.setProperty(PERIODER, ønsketPeriode);
        tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, ønsketÅrsak);
        return tilVurderingTask;
    }

    private List<Fagsak> hentAktuelleFagsaker() {
        return aktuellleFagsakerRepository.hentFagsakerRelevantForMaksdatoVarsel();
    }
}
