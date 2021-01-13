package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BRUK;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;

public class ArbeidsforholdHandlingTypeUtlederTest {

    @Test
    public void skal_utlede_INNTEKT_IKKE_MED_I_BG() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setInntektMedTilBeregningsgrunnlag(false);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        Assertions.assertThrows(IllegalArgumentException.class, () -> ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto));
    }

    @Test
    public void skal_utlede_LAGT_TIL_AV_SAKSBEHANDLER() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setLagtTilAvSaksbehandler(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(LAGT_TIL_AV_SAKSBEHANDLER);
    }

    @Test
    public void skal_utlede_BASERT_PÅ_INNTEKTSMELDING() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBasertPaInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BASERT_PÅ_INNTEKTSMELDING);
    }

    @Test
    public void skal_utlede_BRUK_MED_OVERSTYRT_PERIODE() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setOverstyrtTom(LocalDate.now());
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        Assertions.assertThrows(IllegalArgumentException.class, () -> ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto));
    }

    @Test
    public void skal_utlede_BRUK_UTEN_INNTEKTSMELDING() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BRUK_UTEN_INNTEKTSMELDING);
    }

    @Test
    public void skal_utlede_SLÅTT_SAMMEN_MED_ANNET() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setErstatterArbeidsforholdId("1");

        // Act
        Assertions.assertThrows(IllegalArgumentException.class, () -> ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto));
    }

    @Test
    public void skal_utlede_NYTT_ARBEIDSFORHOLD() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setErNyttArbeidsforhold(true);

        // Act
        Assertions.assertThrows(IllegalArgumentException.class, () -> ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto));
    }

    @Test
    public void skal_utlede_BRUK() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BRUK);
    }
}
