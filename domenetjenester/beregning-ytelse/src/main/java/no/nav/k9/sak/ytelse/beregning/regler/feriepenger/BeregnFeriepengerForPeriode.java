package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;

class BeregnFeriepengerForPeriode {
    private static final BigDecimal FERIEPENGER_SATS_PROSENT = BigDecimal.valueOf(0.102);

    private BeregnFeriepengerForPeriode() {
    }

    static void beregn(Map<String, Object> regelsporing, List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDateInterval feriepengerPeriode, LocalDateInterval feriepengerPeriodeRefusjon, boolean harFeriepengeopptjeningForHelg) {
        beregn(regelsporing, beregningsresultatPerioder, feriepengerPeriode, harFeriepengeopptjeningForHelg, true);
        beregn(regelsporing, beregningsresultatPerioder, feriepengerPeriodeRefusjon, harFeriepengeopptjeningForHelg, false);
    }

    private static void beregn(Map<String, Object> regelsporing, List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDateInterval feriepengerPeriode, boolean harFeriepengeopptjeningForHelg, boolean gjelderBruker) {
        Predicate<BeregningsresultatAndel> andelFilter = andel ->
            andel.getInntektskategori().erArbeidstakerEllerSjømann()
                && andel.erBrukerMottaker() == gjelderBruker
                && andel.getDagsats() > 0;

        for (BeregningsresultatPeriode periode : beregningsresultatPerioder) {
            Optional<LocalDateInterval> overlapp = periode.getPeriode().overlap(feriepengerPeriode);
            if (overlapp.isPresent() && periode.getBeregningsresultatAndelList().stream().anyMatch(andelFilter)) {
                for (LocalDateInterval åretsOverlapp : periodiserPrÅr(overlapp.get())) {
                    long antallFeriepengerDager = harFeriepengeopptjeningForHelg
                        ? beregnKalanderdager(åretsOverlapp)
                        : beregnUkedager(åretsOverlapp);

                    String periodeNavn = "perioden " + åretsOverlapp + "for " + (gjelderBruker ? "bruker" : "arbeidsgiver");
                    regelsporing.put("Antall feriepengedager i " + periodeNavn, antallFeriepengerDager);
                    regelsporing.put("Opptjeningsår i " + periodeNavn, åretsOverlapp.getFomDato().getYear());

                    for (BeregningsresultatAndel andel : periode.getBeregningsresultatAndelList()) {
                        long feriepengerGrunnlag = andel.getDagsats() * antallFeriepengerDager;
                        BigDecimal feriepengerAndelPrÅr = BigDecimal.valueOf(feriepengerGrunnlag).multiply(FERIEPENGER_SATS_PROSENT);
                        if (andelFilter.test(andel) && feriepengerAndelPrÅr.compareTo(BigDecimal.ZERO) != 0) {
                            BeregningsresultatFeriepengerPrÅr.builder()
                                .medOpptjeningÅr(åretsOverlapp.getFomDato().withMonth(12).withDayOfMonth(31))
                                .medÅrsbeløp(feriepengerAndelPrÅr)
                                .build(andel);
                            String mottaker = andel.erBrukerMottaker() ? "Bruker." : "Arbeidsgiver.";
                            String andelId = andel.getArbeidsforhold() != null ? andel.getArbeidsgiverId() : andel.getAktivitetStatus().name();
                            regelsporing.put("Feriepenger." + mottaker + andelId + " i " + periodeNavn, feriepengerAndelPrÅr);
                        }
                    }
                }
            }
        }
    }

    private static long beregnKalanderdager(LocalDateInterval periode) {
        return ChronoUnit.DAYS.between(periode.getFomDato(), periode.getTomDato()) + 1;
    }

    private static int beregnUkedager(LocalDateInterval periode) {
        return beregnUkedager(periode.getFomDato(), periode.getTomDato());
    }

    private static int beregnUkedager(LocalDate fom, LocalDate tom) {
        int antallUkedager = 0;
        for (LocalDate d = fom; !d.isAfter(tom); d = d.plusDays(1)) {
            int dag = d.getDayOfWeek().getValue();
            if (dag <= DayOfWeek.FRIDAY.getValue()) {
                antallUkedager++;
            }
        }
        return antallUkedager;
    }

    private static List<LocalDateInterval> periodiserPrÅr(LocalDateInterval feriepengerPeriode) {
        LocalDate fom = feriepengerPeriode.getFomDato();
        LocalDate tom = feriepengerPeriode.getTomDato();
        List<LocalDateInterval> perioder = new ArrayList<>();
        while (fom.getYear() != tom.getYear()) {
            LocalDate sisteDagIÅr = fom.withMonth(12).withDayOfMonth(31);
            LocalDateInterval dateInterval = new LocalDateInterval(fom, sisteDagIÅr);
            perioder.add(dateInterval);
            fom = sisteDagIÅr.plusDays(1);
        }
        LocalDateInterval dateInterval = new LocalDateInterval(fom, tom);
        perioder.add(dateInterval);
        return perioder;
    }
}
