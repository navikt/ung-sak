package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;

public class UtledOmHistorikkinnslagForInntektsmeldingErNødvendigTest {

    @Test
    public void skal_returne_false_når_inntektsmelding_er_mottatt() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setMottattDatoInntektsmelding(LocalDate.now());
        // Act
        boolean erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.empty());
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_false_når_permisjon_skal_brukes() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        // Act
        boolean erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.empty());
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_false_når_arbeidsforhold_fom_dato_er_lik_stp() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now());
        // Act
        boolean erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.of(LocalDate.now()));
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_false_når_arbeidsforhold_fom_dato_er_etter_stp() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now().plusDays(1));
        // Act
        boolean erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.of(LocalDate.now()));
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_true_når_arbeidsforholdet_ikke_har_mottat_IM_og_arbeidsforhold_starter_før_stp_og_ingen_permisjon() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now().minusDays(1));
        arbeidsforholdDto.setMottattDatoInntektsmelding(null);
        arbeidsforholdDto.setBrukPermisjon(null);
        // Act
        boolean erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.of(LocalDate.now()));
        // Assert
        assertThat(erNødvendig).isTrue();
    }

    @Test
    public void skal_returne_true_når_IM_ikke_mottatt_og_ingen_permisjon_og_ingen_stp() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now().plusDays(1));
        arbeidsforholdDto.setMottattDatoInntektsmelding(null);
        arbeidsforholdDto.setBrukPermisjon(null);
        // Act
        boolean erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.empty());
        // Assert
        assertThat(erNødvendig).isTrue();
    }

}
