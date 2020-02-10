package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;

final class UtledOmHistorikkinnslagForInntektsmeldingErNødvendig {

    private UtledOmHistorikkinnslagForInntektsmeldingErNødvendig() {
        // Skjul default constructor
    }

    static boolean utled(AvklarArbeidsforholdDto arbeidsforholdDto, Optional<LocalDate> stpOpt) {
        if (harMottattInntektsmelding(arbeidsforholdDto)) {
            return false;
        }
        if (skalBrukePermisjon(arbeidsforholdDto)) {
            return false;
        }
        return !arbeidsforholdetStarterPåEllerEtterStp(arbeidsforholdDto, stpOpt);
    }

    private static boolean harMottattInntektsmelding(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return arbeidsforholdDto.getMottattDatoInntektsmelding() != null;
    }

    private static boolean skalBrukePermisjon(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getBrukPermisjon());
    }

    private static boolean arbeidsforholdetStarterPåEllerEtterStp(AvklarArbeidsforholdDto arbeidsforholdDto, Optional<LocalDate> stpOpt) {
        if (stpOpt.isPresent()) {
            final LocalDate stp = stpOpt.get();
            return arbeidsforholdDto.getFomDato().isEqual(stp) || arbeidsforholdDto.getFomDato().isAfter(stp);
        }
        return false;
    }

}
