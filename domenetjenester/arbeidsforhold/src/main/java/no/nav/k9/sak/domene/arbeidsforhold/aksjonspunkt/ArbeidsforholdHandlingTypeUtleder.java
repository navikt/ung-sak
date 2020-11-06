package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BRUK;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.IKKE_BRUK;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.vedtak.util.StringUtils;

class ArbeidsforholdHandlingTypeUtleder {

    private ArbeidsforholdHandlingTypeUtleder() {
        // skjul public constructor
    }

    static ArbeidsforholdHandlingType utledHandling(AvklarArbeidsforholdDto arbeidsforholdDto) {

        if (inntektSkalIkkeMedTilBeregningsgrunnlaget(arbeidsforholdDto)) {
            throw new IllegalStateException("Ugyldig handling: " + INNTEKT_IKKE_MED_I_BG);
        } else if (skalLeggeTilNyttArbeidsforhold(arbeidsforholdDto)) {
            return LAGT_TIL_AV_SAKSBEHANDLER;
        } else if (skalLeggeTilNyttArbeidsforholdBasertPåInntektsmelding(arbeidsforholdDto)) {
            return BASERT_PÅ_INNTEKTSMELDING;
        } else if (skalOverstyrePerioder(arbeidsforholdDto)) {
            throw new IllegalStateException("Ugyldig handling: " + BRUK_MED_OVERSTYRT_PERIODE);
        } else if (skalBrukeUtenInnteksmelding(arbeidsforholdDto)) {
            // TODO: Denne bør sannsynligvis fjernes
            //throw new IllegalStateException("Ugyldig handling: " + BRUK_UTEN_INNTEKTSMELDING);
            return BRUK_UTEN_INNTEKTSMELDING;
        } else if (skalErstatteAnnenInntektsmelding(arbeidsforholdDto)) {
            throw new IllegalStateException("Ugyldig handling: " + SLÅTT_SAMMEN_MED_ANNET);
        } else if (erNyttArbeidsforhold(arbeidsforholdDto)) {
            throw new IllegalStateException("Ugyldig handling: " + NYTT_ARBEIDSFORHOLD);
        } else if (brukArbeidsforholdet(arbeidsforholdDto)) {
            return BRUK;
        }
        return IKKE_BRUK;
    }

    private static boolean skalOverstyrePerioder(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return arbeidsforholdDto.getOverstyrtTom() != null
            && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean inntektSkalIkkeMedTilBeregningsgrunnlaget(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.FALSE.equals(arbeidsforholdDto.getInntektMedTilBeregningsgrunnlag())
            && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean skalBrukeUtenInnteksmelding(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getFortsettBehandlingUtenInntektsmelding())
            && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean skalLeggeTilNyttArbeidsforhold(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getLagtTilAvSaksbehandler())
            && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean skalLeggeTilNyttArbeidsforholdBasertPåInntektsmelding(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getBasertPaInntektsmelding())
            && brukArbeidsforholdet(arbeidsforholdDto);
    }

    static boolean skalErstatteAnnenInntektsmelding(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return !StringUtils.nullOrEmpty(arbeidsforholdDto.getErstatterArbeidsforholdId())
            && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static Boolean erNyttArbeidsforhold(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getErNyttArbeidsforhold())
            && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean brukArbeidsforholdet(AvklarArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getBrukArbeidsforholdet());
    }

}
