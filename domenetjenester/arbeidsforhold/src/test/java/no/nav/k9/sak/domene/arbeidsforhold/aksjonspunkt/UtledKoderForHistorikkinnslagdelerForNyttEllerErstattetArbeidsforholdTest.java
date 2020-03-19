package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

import no.nav.k9.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;

public class UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforholdTest {

    @Test
    public void skal_returne_NYTT_ARBEIDSFORHOLD_når_arbeidsforholdet_har_nytt_arbeidsforhold_satt_til_true() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setErNyttArbeidsforhold(true);
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.NYTT_ARBEIDSFORHOLD)
        );
    }

    @Test
    public void skal_returne_SLÅTT_SAMMEN_MED_ANNET_når_arbeidsforholdet_har_en_arbeidsforhold_id_som_skal_erstattes() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setErstatterArbeidsforholdId("123");
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode ->
            assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SLÅTT_SAMMEN_MED_ANNET)
        );
    }

    @Test
    public void skal_returne_empty_når_arbeidsforholdet_ikke_er_nytt_eller_erstattes() {
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        // Act
        Optional<VurderArbeidsforholdHistorikkinnslag> kodeOpt = UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold.utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).isEmpty();
    }

}
