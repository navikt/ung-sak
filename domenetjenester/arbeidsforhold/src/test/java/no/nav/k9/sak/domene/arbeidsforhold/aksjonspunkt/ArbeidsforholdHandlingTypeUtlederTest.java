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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsforholdHandlingTypeUtleder;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;

public class ArbeidsforholdHandlingTypeUtlederTest {

    @Test
    public void skal_utlede_INNTEKT_IKKE_MED_I_BG() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setInntektMedTilBeregningsgrunnlag(false);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(INNTEKT_IKKE_MED_I_BG);
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
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BRUK_MED_OVERSTYRT_PERIODE);
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
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(SLÅTT_SAMMEN_MED_ANNET);
    }

    @Test
    public void skal_utlede_NYTT_ARBEIDSFORHOLD() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setErNyttArbeidsforhold(true);

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(NYTT_ARBEIDSFORHOLD);
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

    @Test
    public void skal_utlede_IKKE_BRUK_hvis_false() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(false);

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(IKKE_BRUK);
    }

    @Test
    public void skal_utlede_IKKE_BRUK_hvis_null() {
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();

        // Act
        ArbeidsforholdHandlingType resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(IKKE_BRUK);
    }
}
