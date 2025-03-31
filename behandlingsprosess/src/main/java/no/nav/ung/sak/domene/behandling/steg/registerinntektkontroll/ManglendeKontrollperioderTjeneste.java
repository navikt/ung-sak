package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.ytelse.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

@Dependent
public class ManglendeKontrollperioderTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ManglendeKontrollperioderTjeneste.class);

    public static final int RAPPORTERINGSFRIST_I_MÅNED = 7;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private YtelseperiodeUtleder ytelseperiodeUtleder;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    public ManglendeKontrollperioderTjeneste(KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                             YtelseperiodeUtleder ytelseperiodeUtleder,
                                             ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder, ProsessTriggereRepository prosessTriggereRepository) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.ytelseperiodeUtleder = ytelseperiodeUtleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.prosessTriggereRepository = prosessTriggereRepository;
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

        final var manglendeTriggere = mapTilProsessTriggere(manglendeKontrollTidslinje);

        if (!manglendeTriggere.isEmpty()) {
            LOG.info("Legger til manglende triggere for kontroll: {}", manglendeTriggere);
            prosessTriggereRepository.leggTil(behandlingId, manglendeTriggere);
        }
    }

    private static Set<Trigger> mapTilProsessTriggere(LocalDateTimeline<Boolean> manglendeKontrollTidslinje) {
        final var manglendeTriggere = manglendeKontrollTidslinje.compress().getLocalDateIntervals().stream()
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

    private static LocalDateTimeline<Boolean> finnPerioderMedPassertRapporteringsfrist() {
        return new LocalDateTimeline<>(TIDENES_BEGYNNELSE, getTomDatoForPassertRapporteringsfrist(), true);
    }

    private static LocalDateTimeline<Boolean> finnPerioderSomSkalKontrolleres(LocalDateTimeline<Boolean> ytelsesPerioder) {
        LocalDateTimeline<Boolean> perioderForKontroll = LocalDateTimeline.empty();
        if (ytelsesPerioder.toSegments().size() > 2) {
            final var segmenterForKontroll = ytelsesPerioder.toSegments();
            segmenterForKontroll.removeFirst();
            segmenterForKontroll.removeLast();
            perioderForKontroll = new LocalDateTimeline<>(segmenterForKontroll);
        }
        return perioderForKontroll;
    }

    private static LocalDate getTomDatoForPassertRapporteringsfrist() {
        final var dagensDato = LocalDate.now();
        return dagensDato.getDayOfMonth() < RAPPORTERINGSFRIST_I_MÅNED ? dagensDato.minusMonths(2).with(TemporalAdjusters.lastDayOfMonth()) : dagensDato.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

}
