package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.smurt;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;

/**
 * Samler logikk for å nulle ut etablert tilsyn i enkelte tilfeller.
 */
public class EtablertTilsynUnntaksutnuller {

    public static LocalDateTimeline<Duration> ignorerEtablertTilsynVedInnleggelserOgUnntak(LocalDateTimeline<Duration> etablertTilsynForPleietrengende,
            Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende,
            List<PleietrengendeSykdomInnleggelsePeriode> innleggelser) {
        
        LocalDateTimeline<Duration> etablertTilsynResultat = etablertTilsynForPleietrengende;
        etablertTilsynResultat = ignorerEtablertTilsynVedInnleggelser(etablertTilsynResultat, innleggelser);
        etablertTilsynResultat = håndterUnntak(etablertTilsynResultat, unntakEtablertTilsynForPleietrengende);
        
        return etablertTilsynResultat;
    }

    public static LocalDateTimeline<Duration> ignorerEtablertTilsynVedInnleggelser(LocalDateTimeline<Duration> etablertTilsynTidslinje,
            List<PleietrengendeSykdomInnleggelsePeriode> innleggelser) {
        
        if (innleggelser.isEmpty() || etablertTilsynTidslinje.isEmpty()) {
            return etablertTilsynTidslinje;
        }

        final LocalDateTimeline<Boolean> innleggelseTidslinje = new LocalDateTimeline<>(innleggelser.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .collect(Collectors.toList()));

        return TidslinjeUtil.kunPerioderSomIkkeFinnesI(etablertTilsynTidslinje, innleggelseTidslinje);
    }
    
    
    private static LocalDateTimeline<Duration> håndterUnntak(
            LocalDateTimeline<Duration> etablertTilsynTidslinje,
            Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende) {
        if (unntakEtablertTilsynForPleietrengende.isEmpty()) {
            return etablertTilsynTidslinje;
        }
        
        LocalDateTimeline<Duration> tidslinje = etablertTilsynTidslinje;
        tidslinje = håndterUnntak(tidslinje, unntakEtablertTilsynForPleietrengende.get().getNattevåk());
        tidslinje = håndterUnntak(tidslinje, unntakEtablertTilsynForPleietrengende.get().getBeredskap());
        
        return tidslinje;
    }
    
    private static LocalDateTimeline<Duration> håndterUnntak(
            LocalDateTimeline<Duration> etablertTilsynTidslinje,
            UnntakEtablertTilsyn unntak) {
        if (unntak == null || unntak.getPerioder() == null || unntak.getPerioder().isEmpty()) {
            return etablertTilsynTidslinje;
        }
        
        final List<LocalDateSegment<Duration>> segments = unntak.getPerioder().stream()
                .filter(u -> u.getResultat() == Resultat.OPPFYLT)
                .map(u -> new LocalDateSegment<>(u.getPeriode().getFomDato(), u.getPeriode().getTomDato(), Duration.ofHours(0)))
                .collect(Collectors.toList());
        final LocalDateTimeline<Duration> unntakstidslinje = new LocalDateTimeline<>(segments);
        
        return etablertTilsynTidslinje.combine(unntakstidslinje, StandardCombinators::coalesceRightHandSide, JoinStyle.LEFT_JOIN).compress();
    }
}
