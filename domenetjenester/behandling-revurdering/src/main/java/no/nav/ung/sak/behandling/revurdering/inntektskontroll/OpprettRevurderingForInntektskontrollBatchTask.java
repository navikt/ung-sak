package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.vilkår.VilkårTjeneste;


/**
 * Batchtask som starter kontroll av inntekt fra a-inntekt
 * <p>
 * Kjører hver dag kl 07:15.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingForInntektskontrollBatchTask.TASKNAME, cronExpression = "0 0 7 7 * *", maxFailedRuns = 1)
public class OpprettRevurderingForInntektskontrollBatchTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingForInntektskontrollBatch";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingForInntektskontrollBatchTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private VilkårTjeneste vilkårTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    OpprettRevurderingForInntektskontrollBatchTask() {
    }

    @Inject
    public OpprettRevurderingForInntektskontrollBatchTask(ProsessTaskTjeneste prosessTaskTjeneste,
                                                          BehandlingRepository behandlingRepository,
                                                          FagsakRepository fagsakRepository,
                                                          UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                          ProsessTriggereRepository prosessTriggereRepository,
                                                          VilkårTjeneste vilkårTjeneste,
                                                          UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        var tom = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        var fagsaker = fagsakRepository.hentAlleFagsakerSomOverlapper(fom, tom);

        var behandlinger = fagsaker.stream().map(Fagsak::getId).map(fagsakId -> {
            var avsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
            if (avsluttetBehandling.isPresent()) {
                return avsluttetBehandling;
            }
            return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        }).flatMap(Optional::stream).toList();

        /* Filtrere ut behandlinger som skal ha inntektskontroll:
            - Har ikke allerede opprettet trigger for inntektskontroll
            - Har programdeltagelse og den startet ikke forrige måned. Og slutter ikke i inneværende måned
            - Har ikke avslåtte vilkår, men kan ha vilkår som er til vurdering
            - Har ikke avslått uttak, men kan ha uttak som er til vurdering
         */
        var behandlingerTilKontroll = behandlinger.stream()
                .filter(behandling -> harIkkeOpprettetTrigger(behandling, fom, tom))
                .filter(behandling -> harProgramdeltagelseSomGirInntektskontroll(behandling, fom, tom))
                .filter(behandling -> harIkkeAvslåtteVilkår(behandling, fom, tom))
                .filter(behandling -> harIkkeAvslåttUttak(behandling, fom, tom))
                .toList();

        log.info("Fant følgende saker til kontroll av inntekt: ", behandlinger.stream().map(Behandling::getFagsak).map(Fagsak::getSaksnummer).toList());

        // TODO: Legg inn dette når resten er klart (vi vil ikkje kjøre i Q før vi kan teste det skikkelig i Q)
        opprettProsessTask(behandlingerTilKontroll, fom, tom);
    }

    private void opprettProsessTask(List<Behandling> behandlingerTilKontroll, LocalDate fom, LocalDate tom) {
        ProsessTaskGruppe taskGruppeTilRevurderinger = new ProsessTaskGruppe();

        var revurderTasker = behandlingerTilKontroll
                .stream()
                .map(behandling -> {
                    var fagsakId = behandling.getFagsakId();
                    log.info("Oppretter revurdering for fagsak med id {}", fagsakId);

                    ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
                    tilVurderingTask.setFagsakId(fagsakId);
                    tilVurderingTask.setProperty(PERIODER, fom + "/" + tom);
                    tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());
                    return tilVurderingTask;
                }).toList();

        taskGruppeTilRevurderinger.addNesteParallell(revurderTasker);
        prosessTaskTjeneste.lagre(taskGruppeTilRevurderinger);
    }

    private boolean harIkkeAvslåttUttak(Behandling behandling, LocalDate fom, LocalDate tom) {
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId());
        if (ungdomsytelseGrunnlag.isEmpty()) {
            // Hvis ikke vurdert uttak
            return true;
        }
        return ungdomsytelseGrunnlag.get().getAvslagstidslinjeFraUttak().filterValue(it -> it.avslagsårsak() != null).intersection(new LocalDateInterval(fom, tom)).isEmpty();
    }

    private boolean harIkkeAvslåtteVilkår(Behandling behandling, LocalDate fom, LocalDate tom) {
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId()).intersection(new LocalDateInterval(fom, tom));
        var harVilkårSomIkkeErVurdert = samletResultat.toSegments().stream().anyMatch(segment -> segment.getValue().getSamletUtfall().equals(Utfall.IKKE_VURDERT));
        if (samletResultat.isEmpty() || harVilkårSomIkkeErVurdert) {
            //førstegangsbehandling som ikke har fått behandlet vilkår
            return true;
        }
        return erInnvilget(samletResultat);
    }

    private static boolean erInnvilget(LocalDateTimeline<VilkårUtfallSamlet> samletResultat) {
        return samletResultat.toSegments().stream().noneMatch(segment -> segment.getValue().getSamletUtfall().equals(Utfall.IKKE_OPPFYLT));
    }

    // Inntektsrapportering gjelder bare fra måned nr 2 og inkluderer ikke evt. opphørsmåned
    private boolean harProgramdeltagelseSomGirInntektskontroll(Behandling behandling, LocalDate fom, LocalDate tom) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        if (ungdomsprogramPeriodeGrunnlag.isEmpty()) {
            return false;
        }

        var perioder = ungdomsprogramPeriodeGrunnlag.get().getUngdomsprogramPerioder().getPerioder();
        if (perioder.isEmpty()) {
            return false;
        }
        var startdato = perioder.stream().map(p -> p.getPeriode().getFomDato()).min(Comparator.naturalOrder()).orElseThrow();
        var sluttdato = perioder.stream().map(p -> p.getPeriode().getTomDato()).max(Comparator.naturalOrder()).orElseThrow();
        return startdato.isBefore(fom) && sluttdato.isAfter(tom);
    }

    private boolean harIkkeOpprettetTrigger(Behandling behandling, LocalDate fom, LocalDate tom) {
        return prosessTriggereRepository.hentGrunnlag(behandling.getId()).stream().flatMap(it -> it.getTriggere().stream()).noneMatch(it -> it.getÅrsak().equals(RE_KONTROLL_REGISTER_INNTEKT) && it.getPeriode().overlapper(fom, tom));
    }


}
