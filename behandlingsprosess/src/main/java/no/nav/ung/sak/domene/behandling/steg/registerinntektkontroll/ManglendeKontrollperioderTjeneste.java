package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.ytelse.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

@Dependent
public class ManglendeKontrollperioderTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ManglendeKontrollperioderTjeneste.class);

    private final int rapporteringsfristIMåned;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private YtelseperiodeUtleder ytelseperiodeUtleder;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    public ManglendeKontrollperioderTjeneste(KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                             YtelseperiodeUtleder ytelseperiodeUtleder,
                                             ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                             ProsessTriggereRepository prosessTriggereRepository,
                                             @KonfigVerdi(value = "RAPPORTERINGSFRIST_DAG_I_MAANED", defaultVerdi = "6") int rapporteringsfristIMåned) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.ytelseperiodeUtleder = ytelseperiodeUtleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.rapporteringsfristIMåned = rapporteringsfristIMåned;
    }

    /** Legger til triggere for kontroll der denne mangler
     * Dette kan skje dersom programperioden er flyttet langt nok tilbake i tid eller at første søknad er sendt inn etter at rapporteringsfristen for andre måned er passert.
     * @param behandlingId BehandlingId
     */
    public void leggTilManglendeKontrollTriggere(Long behandlingId) {
        final var ytelsesPerioder = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);
        final var påkrevdKontrollTidslinje = finnPerioderSomSkalKontrolleres(ytelsesPerioder);
        final var passertRapporteringsfristTidslinje = finnPerioderMedPassertRapporteringsfrist();
        final var markertForKontrollTidslinje = finnPerioderMarkertForKontroll(behandlingId);
        var utførtKontrollTidslinje = finnPerioderSomErKontrollertITidligereBehandlinger(behandlingId);
        final var manglendeKontrollTidslinje = påkrevdKontrollTidslinje.disjoint(utførtKontrollTidslinje).disjoint(markertForKontrollTidslinje).intersection(passertRapporteringsfristTidslinje);

        final var manglendeTriggere = mapTilProsessTriggere(manglendeKontrollTidslinje, ytelsesPerioder);

        if (!manglendeTriggere.isEmpty()) {
            LOG.info("Legger til manglende triggere for kontroll: {}", manglendeTriggere);
            prosessTriggereRepository.leggTil(behandlingId, manglendeTriggere);
        }
    }

    private static Set<Trigger> mapTilProsessTriggere(LocalDateTimeline<Boolean> manglendeKontrollTidslinje, LocalDateTimeline<Boolean> ytelsesPerioder) {
        final var manglendeTriggere = manglendeKontrollTidslinje.compress()
            .combine(ytelsesPerioder, StandardCombinators::leftOnly, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .getLocalDateIntervals().stream()
            .map(di -> new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(di)))
            .collect(Collectors.toSet());
        return manglendeTriggere;
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

    private static LocalDateTimeline<Boolean> finnPerioderSomSkalKontrolleres(LocalDateTimeline<Boolean> ytelsesPerioder) {
        LocalDateTimeline<Boolean> perioderForKontroll = LocalDateTimeline.empty();
        if (ytelsesPerioder.toSegments().size() > 2) {
            final var segmenterForKontroll = new TreeSet<>(ytelsesPerioder.toSegments());
            segmenterForKontroll.removeFirst();
            segmenterForKontroll.removeLast();
            perioderForKontroll = new LocalDateTimeline<>(segmenterForKontroll);
        }
        return perioderForKontroll;
    }

    private LocalDate getTomDatoForPassertRapporteringsfrist() {
        final var dagensDato = LocalDate.now();
        return dagensDato.getDayOfMonth() <= rapporteringsfristIMåned ? dagensDato.minusMonths(2).with(TemporalAdjusters.lastDayOfMonth()) : dagensDato.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

}
