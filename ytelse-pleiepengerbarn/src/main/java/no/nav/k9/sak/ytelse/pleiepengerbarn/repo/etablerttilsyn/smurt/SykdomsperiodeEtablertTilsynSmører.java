package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.smurt;

import java.time.Duration;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeMedVarighet;

/**
 * Håndterer smøring av omsorgstilbudstimer ut fra oppfylte sykdomsperioder.
 */
public class SykdomsperiodeEtablertTilsynSmører {

    /**
     * Smører omsorgstilbud over oppfylte sykdomsperioder med maksimallengde mandag-fredag.
     *
     * @param sykdomsperioderPåPleietrengende Sykdomsperiodene på den pleietrengende der oppfylte perioder
     *          har verdien {@code true}.
     * @param etablertTilsynForPleietrengende Omsorgstilbudstimene som skal smøres.
     */
    public static List<PeriodeMedVarighet> smørEtablertTilsyn(LocalDateTimeline<Boolean> sykdomsperioderPåPleietrengende,
            LocalDateTimeline<Duration> etablertTilsynForPleietrengende) {

        final LocalDateTimeline<Boolean> smørsingsperiodeTidslinje = toSykdomsuketidslinje(sykdomsperioderPåPleietrengende.filterValue(Boolean::booleanValue));
        final LocalDateTimeline<Duration> etablertTilsynForSmøring = etablertTilsynForPleietrengende.intersection(smørsingsperiodeTidslinje);

        return EtablertTilsynSmører.smørEtablertTilsyn(smørsingsperiodeTidslinje, etablertTilsynForSmøring);
    }


    private static LocalDateTimeline<Boolean> toSykdomsuketidslinje(LocalDateTimeline<Boolean> sykdomstidslinje) {
        if (sykdomstidslinje.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        LocalDateTimeline<Boolean> sykdomsukestidslinje = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(sykdomstidslinje.getMinLocalDate(), sykdomstidslinje.getMaxLocalDate());
        sykdomsukestidslinje = sykdomstidslinje.intersection(sykdomsukestidslinje).compress();
        return sykdomsukestidslinje;
    }

}
