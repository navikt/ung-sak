package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.utils.Hjelpetidslinjer;

/**
 * Smører omsorgstilbud over periode i uke (mandag-fredag) der sykdomsvilkåret har blitt oppfylt.
 */
public final class EtablertTilsynSmører {
    
    private EtablertTilsynSmører() {}

    /**
     * Smører omsorgstilbud over periode i uke (mandag-fredag) der sykdomsvilkåret har blitt oppfylt.
     * 
     * @param sykdomtidslinje En tidslinje over når sykdomsvilkåret er oppfylt på den pleietrengende.
     *          Merk at dette skal være den komplette sykdomstidslinjen på barnet -- og IKKE
     *          oppfylt sykdomsvilkår kun på saken.
     * @param etablertTilsynPerioder De usmurte periodene med omsorgstilbud.
     * @param unntakEtablertTilsynForPleietrengende Unntak fra omsorgstilbud som kan fjerne
     *          omsorgstilbudet for enkelte dager før tallene skal smøres.
     * 
     * @return Resultatet.
     */
    public static List<PeriodeMedVarighet> smørEtablertTilsyn(LocalDateTimeline<Boolean> sykdomtidslinje,
            List<EtablertTilsynPeriode> etablertTilsynPerioder,
            Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende) {
        
        final LocalDateTimeline<Boolean> sykdomsukestidslinje = toSykdomsuketidslinje(sykdomtidslinje.filterValue(Boolean::booleanValue));
        
        LocalDateTimeline<EtablertTilsynPeriode> etablertTilsynTidslinje = toEtablertTilsynPeriodetidslinje(etablertTilsynPerioder);
        etablertTilsynTidslinje = etablertTilsynTidslinje.intersection(sykdomsukestidslinje);
        etablertTilsynTidslinje = håndterUnntak(etablertTilsynTidslinje, unntakEtablertTilsynForPleietrengende);
        
        final List<PeriodeMedVarighet> smurtEtablertTilsynPeriode = new ArrayList<>();
        for (LocalDateSegment<Boolean> smøringsperiode : sykdomsukestidslinje) {
            final List<EtablertTilsynPeriode> perioderTilSmøring = finnPerioderSomSkalSmøres(etablertTilsynTidslinje, smøringsperiode);
            
            if (perioderTilSmøring.isEmpty()) {
                continue;
            }
            
            final Duration smurtVarighet = finnSmurtVarighet(antallDager(smøringsperiode),perioderTilSmøring);
            smurtEtablertTilsynPeriode.add(toPeriodeMedVarighet(smøringsperiode, smurtVarighet));
        }
        return smurtEtablertTilsynPeriode;
    }
    
    private static LocalDateTimeline<EtablertTilsynPeriode> håndterUnntak(
            LocalDateTimeline<EtablertTilsynPeriode> etablertTilsynTidslinje,
            Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende) {
        if (unntakEtablertTilsynForPleietrengende.isEmpty()) {
            return etablertTilsynTidslinje;
        }
        
        LocalDateTimeline<EtablertTilsynPeriode> tidslinje = etablertTilsynTidslinje;
        tidslinje = håndterUnntak(tidslinje, unntakEtablertTilsynForPleietrengende.get().getNattevåk());
        tidslinje = håndterUnntak(tidslinje, unntakEtablertTilsynForPleietrengende.get().getBeredskap());
        
        return tidslinje;
    }
    
    private static LocalDateTimeline<EtablertTilsynPeriode> håndterUnntak(
            LocalDateTimeline<EtablertTilsynPeriode> etablertTilsynTidslinje,
            UnntakEtablertTilsyn unntak) {
        if (unntak == null || unntak.getPerioder() == null || unntak.getPerioder().isEmpty()) {
            return etablertTilsynTidslinje;
        }
        
        final List<LocalDateSegment<EtablertTilsynPeriode>> segments = unntak.getPerioder().stream()
                .filter(u -> u.getResultat() == Resultat.OPPFYLT)
                .map(u -> new LocalDateSegment<>(u.getPeriode().getFomDato(), u.getPeriode().getTomDato(), new EtablertTilsynPeriode(u.getPeriode(), Duration.ofHours(0), null)))
                .collect(Collectors.toList());
        final LocalDateTimeline<EtablertTilsynPeriode> unntakstidslinje = new LocalDateTimeline<>(segments);
        
        return etablertTilsynTidslinje.combine(unntakstidslinje, StandardCombinators::coalesceRightHandSide, JoinStyle.LEFT_JOIN).compress();
    }

    private static Duration finnSmurtVarighet(long antallDager, List<EtablertTilsynPeriode> perioderTilSmøring) {
        final Duration smurtVarighet = perioderTilSmøring.stream()
                .map(p -> p.getVarighet().multipliedBy(antallDager(p)))
                .reduce(Duration.ofHours(0), (a, b) -> a.plus(b))
                .dividedBy(antallDager);
        
        return smurtVarighet;
    }

    private static long antallDager(EtablertTilsynPeriode p) {
        return ChronoUnit.DAYS.between(p.getPeriode().getFomDato(), p.getPeriode().getTomDato().plusDays(1));
    }
    
    private static long antallDager(LocalDateSegment<?> p) {
        return ChronoUnit.DAYS.between(p.getFom(), p.getTom().plusDays(1));
    }

    private static List<EtablertTilsynPeriode> finnPerioderSomSkalSmøres(
            LocalDateTimeline<EtablertTilsynPeriode> etablertTilsynTidslinje,
            LocalDateSegment<Boolean> smøringsperiode) {
        final List<EtablertTilsynPeriode> perioderTilSmøring = etablertTilsynTidslinje.intersection(smøringsperiode.getLocalDateInterval())
                .stream()
                .map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), new EtablertTilsynPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getVarighet(), null)
                ))
                .map(LocalDateSegment::getValue)
                .collect(Collectors.toList());
        return perioderTilSmøring;
    }

    private static LocalDateTimeline<EtablertTilsynPeriode> toEtablertTilsynPeriodetidslinje(
            List<EtablertTilsynPeriode> etablertTilsynPerioder) {
        LocalDateTimeline<EtablertTilsynPeriode> etablertTilsynTidslinje = new LocalDateTimeline<>(etablertTilsynPerioder.stream()
                .map(e -> new LocalDateSegment<>(e.getPeriode().getFomDato(), e.getPeriode().getTomDato(), e))
                .collect(Collectors.toList()));
        return etablertTilsynTidslinje;
    }

    private static LocalDateTimeline<Boolean> toSykdomsuketidslinje(LocalDateTimeline<Boolean> sykdomstidslinje) {
        LocalDateTimeline<Boolean> sykdomsukestidslinje = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(sykdomstidslinje.getMinLocalDate(), sykdomstidslinje.getMaxLocalDate());
        sykdomsukestidslinje = sykdomstidslinje.intersection(sykdomsukestidslinje).compress();
        return sykdomsukestidslinje;
    }
        
    private static PeriodeMedVarighet toPeriodeMedVarighet(LocalDateSegment<Boolean> smøringsperiode,
            final Duration smurtVarighet) {
        return new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(smøringsperiode.getFom(),  smøringsperiode.getTom()), smurtVarighet);
    }
}
