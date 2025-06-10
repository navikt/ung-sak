package no.nav.ung.sak.ytelse.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

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
    public Set<LocalDateInterval> finnPerioderForManglendeKontroll(Long behandlingId) {
        final var månedsvisYtelsestidslinje = månedsvisTidslinjeUtleder.periodiserMånedsvis(behandlingId);
        final var påkrevdKontrollTidslinje = RelevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(månedsvisYtelsestidslinje);
        final var passertRapporteringsfristTidslinje = finnPerioderMedPassertRapporteringsfrist();
        final var markertForKontrollTidslinje = finnPerioderMarkertForKontroll(behandlingId);
        var utførtKontrollTidslinje = finnPerioderSomErKontrollertITidligereBehandlinger(behandlingId);
        final var manglendeKontrollTidslinje = påkrevdKontrollTidslinje.disjoint(utførtKontrollTidslinje).disjoint(markertForKontrollTidslinje).intersection(passertRapporteringsfristTidslinje);
        return splittPåMåneder(manglendeKontrollTidslinje, månedsvisYtelsestidslinje);

    }


    private static Set<LocalDateInterval> splittPåMåneder(LocalDateTimeline<Boolean> manglendeKontrollTidslinje, LocalDateTimeline<YearMonth> månedsvisYtelsestidslinje) {
        return manglendeKontrollTidslinje.compress()
            .combine(månedsvisYtelsestidslinje, StandardCombinators::leftOnly, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .getLocalDateIntervals();
    }

    private LocalDateTimeline<Set<BehandlingÅrsakType>> finnPerioderMarkertForKontroll(Long behandlingId) {
        return prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId).filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
    }

    private LocalDateTimeline<Boolean> finnPerioderSomErKontrollertITidligereBehandlinger(Long behandlingId) {
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
