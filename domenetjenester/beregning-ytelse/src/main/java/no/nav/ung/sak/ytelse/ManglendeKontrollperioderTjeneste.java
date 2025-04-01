package no.nav.ung.sak.ytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import no.nav.ung.sak.ytelseperioder.YtelsesperiodeDefinisjon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

@Dependent
public class ManglendeKontrollperioderTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ManglendeKontrollperioderTjeneste.class);

    private final int rapporteringsfristIMåned;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private YtelseperiodeUtleder ytelseperiodeUtleder;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private BehandlingRepository behandlingRepository;

    @Inject
    public ManglendeKontrollperioderTjeneste(BehandlingRepository behandlingRepository,
                                             KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                             YtelseperiodeUtleder ytelseperiodeUtleder,
                                             ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                             @KonfigVerdi(value = "RAPPORTERINGSFRIST_DAG_I_MAANED", defaultVerdi = "6") int rapporteringsfristIMåned) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.ytelseperiodeUtleder = ytelseperiodeUtleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapporteringsfristIMåned = rapporteringsfristIMåned;
        this.behandlingRepository = behandlingRepository;
    }

    /**
     * Lager prosesstaskdata for revurdering grunnet manglende kontroll av inntekt
     * Dette kan skje dersom programperioden er flyttet langt nok tilbake i tid eller at første søknad er sendt inn etter at rapporteringsfristen for andre måned er passert.
     *
     * @param behandlingId BehandlingId
     * @return
     */
    public Optional<ProsessTaskData> lagProsesstaskForRevurderingGrunnetManglendeKontrollAvInntekt(Long behandlingId) {
        final var ytelsesPerioder = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);
        final var påkrevdKontrollTidslinje = RelevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(ytelsesPerioder);
        final var passertRapporteringsfristTidslinje = finnPerioderMedPassertRapporteringsfrist();
        final var markertForKontrollTidslinje = finnPerioderMarkertForKontroll(behandlingId);
        var utførtKontrollTidslinje = finnPerioderSomErKontrollertITidligereBehandlinger(behandlingId);
        final var manglendeKontrollTidslinje = påkrevdKontrollTidslinje.disjoint(utførtKontrollTidslinje).disjoint(markertForKontrollTidslinje).intersection(passertRapporteringsfristTidslinje);

        final var perioderMedManglendeKontroll = splittPåYtelsesperioder(manglendeKontrollTidslinje, ytelsesPerioder);

        if (!perioderMedManglendeKontroll.isEmpty()) {
            final var behandling = behandlingRepository.hentBehandling(behandlingId);
            return Optional.of(lagProsesstask(behandling.getFagsakId(), perioderMedManglendeKontroll));
        } else {
            return Optional.empty();
        }
    }


    private ProsessTaskData lagProsesstask(Long fagsakId, Set<LocalDateInterval> perioder) {
        LOG.info("Oppretter revurdering for fagsak med id {} for perioder {}", fagsakId, perioder);
        ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilVurderingTask.setFagsakId(fagsakId);
        final var perioderString = perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato())
            .collect(Collectors.joining("|"));
        tilVurderingTask.setProperty(PERIODER, perioderString);
        tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());
        return tilVurderingTask;
    }


    private static Set<LocalDateInterval> splittPåYtelsesperioder(LocalDateTimeline<Boolean> manglendeKontrollTidslinje, LocalDateTimeline<YtelsesperiodeDefinisjon> ytelsesPerioder) {
        return manglendeKontrollTidslinje.compress()
            .combine(ytelsesPerioder, StandardCombinators::leftOnly, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .getLocalDateIntervals();
    }

    private LocalDateTimeline<Set<BehandlingÅrsakType>> finnPerioderMarkertForKontroll(Long behandlingId) {
        return prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId).filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
    }

    private LocalDateTimeline<Boolean> finnPerioderSomErKontrollertITidligereBehandlinger(Long behandlingId) {
        return kontrollerteInntektperioderTjeneste.hentTidslinje(behandlingId).mapValue(it -> true);
    }

    private LocalDateTimeline<Boolean> finnPerioderMedPassertRapporteringsfrist() {
        return new LocalDateTimeline<>(TIDENES_BEGYNNELSE, getTomDatoForPassertRapporteringsfrist(), true);
    }

    private LocalDate getTomDatoForPassertRapporteringsfrist() {
        final var dagensDato = LocalDate.now();
        return dagensDato.getDayOfMonth() <= rapporteringsfristIMåned ? dagensDato.minusMonths(2).with(TemporalAdjusters.lastDayOfMonth()) : dagensDato.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

}
