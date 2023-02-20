package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.smurt;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeMedVarighet;

/**
 * Smører omsorgstilbud over flere dager ved å ta gjennomsnittet.
 */
public final class EtablertTilsynSmører {
    
    private EtablertTilsynSmører() {}

    /**
     * Smører omsorgstilbud over perioder definert av {@code smøringsperioder}.
     * 
     * @param smøringsperioder En tidslinje over hvilke perioder som det etablerte tilsynet
     *          skal smøres utover.
     * @param etablertTilsynTidslinje De usmurte periodene med omsorgstilbud.
     * @return Resultatet.
     */
    static List<PeriodeMedVarighet> smørEtablertTilsyn(LocalDateTimeline<Boolean> smøringsperioder,
            LocalDateTimeline<Duration> etablertTilsynTidslinje) {
                
        final List<PeriodeMedVarighet> smurtEtablertTilsynPeriode = new ArrayList<>();
        for (LocalDateSegment<Boolean> smøringsperiode : smøringsperioder) {
            final List<PeriodeMedVarighet> perioderTilSmøring = finnPerioderSomSkalSmøres(etablertTilsynTidslinje, smøringsperiode);
            
            if (perioderTilSmøring.isEmpty()) {
                continue;
            }
            
            final Duration smurtVarighet = finnSmurtVarighet(antallDager(smøringsperiode), perioderTilSmøring);
            smurtEtablertTilsynPeriode.add(toPeriodeMedVarighet(smøringsperiode, smurtVarighet));
        }
        return smurtEtablertTilsynPeriode;
    }

    
    private static Duration finnSmurtVarighet(long antallDager, List<PeriodeMedVarighet> perioderTilSmøring) {
        final Duration smurtVarighet = perioderTilSmøring.stream()
                .map(p -> p.getVarighet().multipliedBy(antallDager(p)))
                .reduce(Duration.ofHours(0), (a, b) -> a.plus(b))
                .dividedBy(antallDager);
        
        return smurtVarighet;
    }

    private static long antallDager(PeriodeMedVarighet p) {
        return ChronoUnit.DAYS.between(p.getPeriode().getFomDato(), p.getPeriode().getTomDato().plusDays(1));
    }
    
    private static long antallDager(LocalDateSegment<?> p) {
        return ChronoUnit.DAYS.between(p.getFom(), p.getTom().plusDays(1));
    }

    private static List<PeriodeMedVarighet> finnPerioderSomSkalSmøres(
            LocalDateTimeline<Duration> etablertTilsynTidslinje,
            LocalDateSegment<Boolean> smøringsperiode) {
        final List<PeriodeMedVarighet> perioderTilSmøring = etablertTilsynTidslinje.intersection(smøringsperiode.getLocalDateInterval())
                .stream()
                .map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), new PeriodeMedVarighet(
                    DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue())
                ))
                .map(LocalDateSegment::getValue)
                .collect(Collectors.toList());
        return perioderTilSmøring;
    }
        
    private static PeriodeMedVarighet toPeriodeMedVarighet(LocalDateSegment<Boolean> smøringsperiode,
            final Duration smurtVarighet) {
        return new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(smøringsperiode.getFom(),  smøringsperiode.getTom()), smurtVarighet);
    }
}
