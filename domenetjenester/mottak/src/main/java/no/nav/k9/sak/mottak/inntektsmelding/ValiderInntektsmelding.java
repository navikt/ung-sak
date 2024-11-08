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
            throw MottattInntektsmeldingException.inntektsmeldingSemantiskValideringFeil(String.format("Angitt [%s] %s>=%s for endringsdato [%s]", path, value, MAX, endringsdato));
        }
        return value;
    }

    public BigDecimal validerMaksBeløp(String path, BigDecimal value) {
        if (value != null && MAX.compareTo(value) <= 0) {
            throw MottattInntektsmeldingException.inntektsmeldingSemantiskValideringFeil(String.format("Angitt [%s] %s>=%s", path, value, MAX));
        }
        return value;
    }

    public List<PeriodeAndel> validerOppgittFravær(List<PeriodeAndel> perioder) {
        if (perioder == null || perioder.isEmpty()) {
            return perioder;
        }


        var segmenter = perioder.stream().map(p -> new LocalDateSegment<PeriodeAndel>(p.getFom(), p.getTom(), p)).collect(Collectors.toList());
        var timeline = new LocalDateTimeline<PeriodeAndel>(segmenter);

        var maksDato = timeline.getMaxLocalDate();
        var minDato = timeline.getMinLocalDate();
        var validerDato = LocalDate.now(); // sjekker mot dagens dato istdf. mottattDato da fordel legger ting på vent inntil da før vi prosesserer.

        if (maksDato.getYear() < CUTOFF_YEAR) {
            // behandler ikke inntektsmeldinger før 2020 her, sendes infotrygd fra fordel i stedet.
            throw MottattInntektsmeldingException.inntektsmeldingSemantiskValideringFeil(String.format("Inntektsmelding gjelder tidligere år: [%s, %s]", minDato, maksDato));
        } else if (maksDato.isAfter(validerDato)) {
            throw MottattInntektsmeldingException.inntektsmeldingSemantiskValideringFeil(String.format("Inntektsmelding oppgitt fravær frem i tid: validerDato=%s, oppgittFravær=[%s, %s]", validerDato, minDato, maksDato));
        }

        return perioder;
    }
}
