package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;

class FinnAktivitetsgrad {

    static BigDecimal finnAktivitetsgrad(Utbetalingsgrader utbetalingsgrader) {
        // Mapper alle 0/0 aktiviteter til 100% aktivitetsgrad sidan vi antar ingen fravær
        if (utbetalingsgrader.getNormalArbeidstid().isZero() && (utbetalingsgrader.getFaktiskArbeidstid() == null || utbetalingsgrader.getFaktiskArbeidstid().isZero())) {
            return BigDecimal.valueOf(100);
        }

        if (utbetalingsgrader.getNormalArbeidstid().isZero()) {
            return new BigDecimal(100).subtract(utbetalingsgrader.getUtbetalingsgrad());
        }

        final Duration faktiskArbeidstid;
        if (utbetalingsgrader.getFaktiskArbeidstid() != null) {
            faktiskArbeidstid = utbetalingsgrader.getFaktiskArbeidstid();
        } else {
            faktiskArbeidstid = Duration.ofHours(0L);
        }

        final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);

        /*
         * XXX: Dette er samme måte å regne ut på som i uttak. På sikt bør vi nok flytte
         *      denne logikken til pleiepenger-barn-uttak.
         */
        final BigDecimal aktivitetsgrad = new BigDecimal(faktiskArbeidstid.toMillis()).setScale(2, RoundingMode.HALF_DOWN)
            .divide(new BigDecimal(utbetalingsgrader.getNormalArbeidstid().toMillis()), 2, RoundingMode.HALF_DOWN)
            .multiply(HUNDRE_PROSENT);

        if (aktivitetsgrad.compareTo(HUNDRE_PROSENT) >= 0) {
            return HUNDRE_PROSENT;
        }

        return aktivitetsgrad;
    }


    static BigDecimal finnAktivitetsgrad(ArbeidsforholdPeriodeInfo arbeidsforholdPeriodeInfo) {
        // Mapper alle 0/0 aktiviteter til 100% aktivitetsgrad sidan vi antar ingen fravær
        if (arbeidsforholdPeriodeInfo.getJobberNormalt().isZero() && arbeidsforholdPeriodeInfo.getJobberNå().isZero()) {
            return BigDecimal.valueOf(100);
        }

        if (arbeidsforholdPeriodeInfo.getJobberNormalt().isZero()) {
            return BigDecimal.ZERO;
        }

        final Duration faktiskArbeidstid;
        faktiskArbeidstid = arbeidsforholdPeriodeInfo.getJobberNå();

        final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);

        /*
         * XXX: Dette er samme måte å regne ut på som i uttak. På sikt bør vi nok flytte
         *      denne logikken til pleiepenger-barn-uttak.
         */
        final BigDecimal aktivitetsgrad = new BigDecimal(faktiskArbeidstid.toMillis()).setScale(2, RoundingMode.HALF_DOWN)
            .divide(new BigDecimal(arbeidsforholdPeriodeInfo.getJobberNormalt().toMillis()), 2, RoundingMode.HALF_DOWN)
            .multiply(HUNDRE_PROSENT);

        if (aktivitetsgrad.compareTo(HUNDRE_PROSENT) >= 0) {
            return HUNDRE_PROSENT;
        }

        return aktivitetsgrad;
    }


}
