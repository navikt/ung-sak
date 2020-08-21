package no.nav.k9.sak.mottak.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;

public class ValiderInntektsmelding {

    private static final BigDecimal MAX = BigDecimal.valueOf(10_000_000L); // månedslønn på 10M maks
    private static final int CUTOFF_YEAR = 2020;

    public BigDecimal validerRefusjonEndringMaks(String path, BigDecimal value, LocalDate endringsdato) {
        if (value != null && MAX.compareTo(value) <= 0) {
            throw MottattInntektsmeldingFeil.FACTORY.inntektsmeldingSemantiskValideringFeil(String.format("Angitt [%s] %s>=%s for endringsdato [%s]", path, value, MAX, endringsdato)).toException();
        }
        return value;
    }

    public BigDecimal validerRefusjonMaks(String path, BigDecimal value) {
        if (value != null && MAX.compareTo(value) <= 0) {
            throw MottattInntektsmeldingFeil.FACTORY.inntektsmeldingSemantiskValideringFeil(String.format("Angitt [%s] %s>=%s", path, value, MAX)).toException();
        }
        return value;
    }

    public List<PeriodeAndel> validerOppgittFravær(LocalDate mottattDato, List<PeriodeAndel> perioder) {
        if (perioder == null || perioder.isEmpty()) {
            return perioder;
        }

        var segmenter = perioder.stream().map(p -> new LocalDateSegment<PeriodeAndel>(p.getFom(), p.getTom(), p)).collect(Collectors.toList());
        var timeline = new LocalDateTimeline<PeriodeAndel>(segmenter);

        var maksDato = timeline.getMaxLocalDate();
        var minDato = timeline.getMinLocalDate();

        if (maksDato.getYear() < CUTOFF_YEAR) {
            // behandler ikke inntektsmeldinger før 2020 her, sendes infotrygd fra fordel i stedet.
            throw MottattInntektsmeldingFeil.FACTORY.inntektsmeldingSemantiskValideringFeil(String.format("Inntektsmelding gjelder tidligere år: [%s, %s]", minDato, maksDato)).toException();
        } else if (maksDato.isAfter(mottattDato)) {
            throw MottattInntektsmeldingFeil.FACTORY.inntektsmeldingSemantiskValideringFeil(String.format("Inntektsmelding oppgitt fravær frem i tid: mottattDato=%s, oppgittFravær=[%s, %s]", mottattDato, minDato, maksDato)).toException();
        }

        return perioder;
    }
}
