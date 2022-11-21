package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class PleiepengerEndretUtbetalingPeriodeutleder implements EndretUtbetalingPeriodeutleder {

    private UttakTjeneste uttakRestKlient;
    private BehandlingRepository behandlingRepository;
    private boolean enabled;


    public PleiepengerEndretUtbetalingPeriodeutleder() {
    }

    @Inject
    public PleiepengerEndretUtbetalingPeriodeutleder(UttakTjeneste uttakRestKlient,
                                                     BehandlingRepository behandlingRepository,
                                                     @KonfigVerdi(value = "BG_FORLENGELSE_BASERT_PAA_UTTAK", defaultVerdi = "false") boolean enabled) {
        this.uttakRestKlient = uttakRestKlient;
        this.behandlingRepository = behandlingRepository;
        this.enabled = enabled;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {

        if (!enabled) {
            return new TreeSet<>(Set.of(periode));
        }

        var originalBehandlingId = behandlingReferanse.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Forventer å finne original behandling"));
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);

        var uttaksplan = uttakRestKlient.hentUttaksplan(behandlingReferanse.getBehandlingUuid(), true);
        var originalUttakslpan = uttakRestKlient.hentUttaksplan(originalBehandling.getUuid(), true);

        var uttakTidslinje = lagTidslinje(uttaksplan);
        var originalUttakTidslinje = lagTidslinje(originalUttakslpan);

        // Må bruke difference begge veier for å finne både nye arbeidsforhold og eventuelt fjernede arbeidsforhold
        var differanse1 = uttakTidslinje.combine(originalUttakTidslinje, StandardCombinators::difference, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var differanse2 = originalUttakTidslinje.combine(uttakTidslinje, StandardCombinators::difference, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var periodeTidslinje = new LocalDateTimeline<>(periode.toLocalDateInterval(), Boolean.TRUE);

        var resultat = new TreeSet<DatoIntervallEntitet>();

        periodeTidslinje.intersection(differanse1, StandardCombinators::rightOnly).toSegments().stream()
            .filter(s -> !s.getValue().isEmpty())
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()))
            .filter(periode::overlapper)
            .forEach(resultat::add);
        periodeTidslinje.intersection(differanse2, StandardCombinators::rightOnly).toSegments().stream()
            .filter(s -> !s.getValue().isEmpty())
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()))
            .filter(periode::overlapper)
            .forEach(resultat::add);

        return resultat;
    }

    private LocalDateTimeline<Set<Utbetalingsgrader>> lagTidslinje(Uttaksplan uttaksplan) {
        Set<LocalDateSegment<Set<Utbetalingsgrader>>> segmenter = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .map(e -> new LocalDateSegment<>(e.getKey().getFom(), e.getKey().getTom(),
                e.getValue().getUtbetalingsgrader().stream().collect(Collectors.toSet())))
            .collect(Collectors.toSet());

        return new LocalDateTimeline<>(segmenter);
    }


}
