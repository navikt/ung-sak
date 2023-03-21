package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;

class BortfaltOgTilkommetArbeidsgiver {

    /**
     * Utleder tidslinje med totalt antall arbeidsgivere
     *
     * @param alleYrkesaktiviteter Alle yrkesaktiviteter
     * @return Tidslinje med antall arbeidsgivere
     */
    static LocalDateTimeline<BigDecimal> utledAntallArbeidsgivereTidslinje(Collection<Yrkesaktivitet> alleYrkesaktiviteter) {
        var gruppertPåArbeidsgiver = alleYrkesaktiviteter
            .stream()
            .filter(it -> ArbeidType.AA_REGISTER_TYPER.contains(it.getArbeidType()))
            .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver));
        return gruppertPåArbeidsgiver.values().stream()
            .map(BortfaltOgTilkommetArbeidsgiver::mapYrkesaktivTidslinje)
            .reduce((t1, t2) -> t1.combine(t2, StandardCombinators::sum, LocalDateTimeline.JoinStyle.CROSS_JOIN))
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDateTimeline<BigDecimal> mapYrkesaktivTidslinje(List<Yrkesaktivitet> yrkesaktiviteter) {
        return yrkesaktiviteter.stream().map(BortfaltOgTilkommetArbeidsgiver::mapAktivTidslinjeForYrkesaktivitet)
            .reduce((t1, t2) -> t1.combine(t2, StandardCombinators::coalesceLeftHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN))
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDateTimeline<BigDecimal> mapAktivTidslinjeForYrkesaktivitet(Yrkesaktivitet yrkesaktivitet) {
        var segmenter = yrkesaktivitet.getAnsettelsesPeriode()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), BigDecimal.ONE))
            .toList();

        LocalDateTimeline<BigDecimal> aktivtsArbeidsforholdTidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceLeftHandSide);

        // Ta bort permisjoner
        var permitteringsTidslinje = PermitteringTidslinjeTjeneste.mapPermittering(yrkesaktivitet);
        return aktivtsArbeidsforholdTidslinje.disjoint(permitteringsTidslinje);
    }

    /**
     * Utleder segmenter med tilkommet aktivitet med ny arbeidsgiver for hver periode
     * <p>
     * Kjører compress av tidslinjen som vurderes slik at sammenhengende segmenter vurderes som en periode.
     *
     * @param antallArbeidsgivereTidslinje Tidslinje for antall arbeidsgivere (se no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.BortfaltOgTilkommetArbeidsgiver#utledAntallArbeidsgivereTidslinje(java.util.Collection))
     * @param tidslinjeSomVurderes         Tidslinje for for perioder som vurderes
     * @return Tidslinje for tilkommet arbeidsgivere pr inaktiv segment
     */
    static LocalDateTimeline<Boolean> utledTilkomneArbeidsgivereTidslinje(LocalDateTimeline<BigDecimal> antallArbeidsgivereTidslinje, LocalDateTimeline<Boolean> tidslinjeSomVurderes) {
        return tidslinjeSomVurderes.compress().map(s -> finnSegmenterUtenReduksjonIAntallArbeidsgivere(antallArbeidsgivereTidslinje, s));
    }


    private static List<LocalDateSegment<Boolean>> finnSegmenterUtenReduksjonIAntallArbeidsgivere(LocalDateTimeline<BigDecimal> antallArbeidsgivereTidslinje, LocalDateSegment<Boolean> s) {
        if (s.getValue()) {
            var antallAktiviteterFørStart = finnAntallAktiviteterFørStart(antallArbeidsgivereTidslinje, s);
            return finnSegmenterUtenReduksjonIAntallArbeidsgivere(antallArbeidsgivereTidslinje, s, antallAktiviteterFørStart);
        } else {
            return Collections.emptyList();
        }
    }

    private static List<LocalDateSegment<Boolean>> finnSegmenterUtenReduksjonIAntallArbeidsgivere(LocalDateTimeline<BigDecimal> antallArbeidsgivereTidslinje, LocalDateSegment<Boolean> s, BigDecimal antallAktiviteterVedStartAvInaktiv) {
        var segmenterUtenReduksjonIAntallArbeidsgivere = antallArbeidsgivereTidslinje.intersection(s.getLocalDateInterval())
            .stream().filter(it -> it.getValue().compareTo(antallAktiviteterVedStartAvInaktiv) >= 0)
            .toList();
        return segmenterUtenReduksjonIAntallArbeidsgivere.stream()
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .toList();
    }

    private static BigDecimal finnAntallAktiviteterFørStart(LocalDateTimeline<BigDecimal> antallArbeidsgivereTidslinje, LocalDateSegment<Boolean> s) {
        var dagenFørStart = s.getFom().minusDays(1);
        var aktiviteterFørStart = antallArbeidsgivereTidslinje.getSegment(new LocalDateInterval(dagenFørStart, dagenFørStart));
        return aktiviteterFørStart != null ? aktiviteterFørStart.getValue() : BigDecimal.ZERO;
    }


}
