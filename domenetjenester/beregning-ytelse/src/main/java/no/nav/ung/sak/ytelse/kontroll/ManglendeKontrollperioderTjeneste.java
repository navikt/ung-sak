package no.nav.ung.sak.ytelse.kontroll;

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
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

@Dependent
public class ManglendeKontrollperioderTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ManglendeKontrollperioderTjeneste.class);

    private final int rapporteringsfristIMåned;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private BehandlingRepository behandlingRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    public ManglendeKontrollperioderTjeneste(BehandlingRepository behandlingRepository,
                                             MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder,
                                             ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                             @KonfigVerdi(value = "RAPPORTERINGSFRIST_DAG_I_MAANED", defaultVerdi = "6") int rapporteringsfristIMåned,
                                             TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapporteringsfristIMåned = rapporteringsfristIMåned;
        this.behandlingRepository = behandlingRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    /**
     * Lager prosesstaskdata for revurdering grunnet manglende kontroll av inntekt
     * Dette kan skje dersom programperioden er flyttet langt nok tilbake i tid eller at første søknad er sendt inn etter at rapporteringsfristen for andre måned er passert.
     *
     * @param behandlingId BehandlingId
     * @return
     */
    public Optional<ProsessTaskData> lagProsesstaskForRevurderingGrunnetManglendeKontrollAvInntekt(Long behandlingId, Long fagsakId) {
        // Månedsvis oppdelt tidslinje for programperioden/fagsakperioden
        final var månedsvisYtelsestidslinje = månedsvisTidslinjeUtleder.periodiserMånedsvis(behandlingId);
        // Tidslinje der det er påkrevd kontroll før utbetaling
        final var påkrevdKontrollTidslinje = RelevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(månedsvisYtelsestidslinje);
        // Tidslinje der rapporteringsfristen er passert
        final var passertRapporteringsfristTidslinje = finnPerioderMedPassertRapporteringsfrist();
        // Tidslinje for alleredee kontrollerte perioder
        var utførtKontrollTidslinje = finnPerioderDerKontrollErGjennomført(behandlingId);

        // Finner tidslinje der det enten er påkrevd kontroll eller det er utført kontroll i åpen behandling
        final var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        var tilKontrollIÅpenBehandlingTidslinje = new LocalDateTimeline<Boolean>(List.of());
        if (sisteBehandling.isPresent() && !sisteBehandling.get().erStatusFerdigbehandlet()) {
            final var sisteBehandlingId = sisteBehandling.get().getId();
            final var kontrollertePerioderISisteBehandling = finnPerioderDerKontrollErGjennomført(behandlingId);
            final var markertForKontrollISisteBehandling = finnPerioderMarkertForKontroll(sisteBehandlingId);
            tilKontrollIÅpenBehandlingTidslinje = markertForKontrollISisteBehandling.crossJoin(kontrollertePerioderISisteBehandling);
        }


        final var manglendeKontrollTidslinje = påkrevdKontrollTidslinje.disjoint(utførtKontrollTidslinje)
            .disjoint(tilKontrollIÅpenBehandlingTidslinje)
            .intersection(passertRapporteringsfristTidslinje);

        final var perioderMedManglendeKontroll = splittPåMåneder(manglendeKontrollTidslinje, månedsvisYtelsestidslinje);

        if (!perioderMedManglendeKontroll.isEmpty()) {
            final var behandling = behandlingRepository.hentBehandling(behandlingId);
            return Optional.of(lagProsesstask(behandling.getFagsakId(), perioderMedManglendeKontroll));
        } else {
            return Optional.empty();
        }
    }


    private ProsessTaskData lagProsesstask(Long fagsakId, Set<LocalDateInterval> perioder) {
        LOG.info("Oppretter revurdering for fagsak med id {} for perioder {} grunnet manglende kontroll", fagsakId, perioder);
        ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilVurderingTask.setFagsakId(fagsakId);
        final var perioderString = perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato())
            .collect(Collectors.joining("|"));
        tilVurderingTask.setProperty(PERIODER, perioderString);
        tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());
        return tilVurderingTask;
    }


    private static Set<LocalDateInterval> splittPåMåneder(LocalDateTimeline<Boolean> manglendeKontrollTidslinje, LocalDateTimeline<YearMonth> månedsvisYtelsestidslinje) {
        return manglendeKontrollTidslinje.compress()
            .combine(månedsvisYtelsestidslinje, StandardCombinators::leftOnly, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .getLocalDateIntervals();
    }

    private LocalDateTimeline<Boolean> finnPerioderMarkertForKontroll(Long behandlingId) {
        return prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId).filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)).mapValue(it -> true);
    }

    private LocalDateTimeline<Boolean> finnPerioderDerKontrollErGjennomført(Long behandlingId) {
        return tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandlingId).mapValue(it -> true);
    }

    private LocalDateTimeline<Boolean> finnPerioderMedPassertRapporteringsfrist() {
        return new LocalDateTimeline<>(TIDENES_BEGYNNELSE, getTomDatoForPassertRapporteringsfrist(), true);
    }

    private LocalDate getTomDatoForPassertRapporteringsfrist() {
        final var dagensDato = LocalDate.now();
        return dagensDato.getDayOfMonth() <= rapporteringsfristIMåned ? dagensDato.minusMonths(2).with(TemporalAdjusters.lastDayOfMonth()) : dagensDato.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

}
