package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapRefusjonskravFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Refusjonskrav;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;

public class MapRefusjonskravFraVLTilRegelTest {
    @Test
    public void refusjonFraSenereDato() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.now();
        LocalDate endringFom = skjæringstidspunkt.plusMonths(1);
        Inntektsmelding inntektsmeldingEntitet = InntektsmeldingBuilder.builder()
            .medRefusjon(BigDecimal.ZERO)
            .leggTil(new Refusjon(BigDecimal.TEN, endringFom))
            .build();

        // Act
        List<Refusjonskrav> resultat = MapRefusjonskravFraVLTilRegel.periodiserRefusjonsbeløp(inntektsmeldingEntitet, skjæringstidspunkt);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat).anySatisfy(start -> {
            assertThat(start.getPeriode()).isEqualTo(Periode.of(skjæringstidspunkt, endringFom.minusDays(1)));
            assertThat(start.getMånedsbeløp()).isEqualByComparingTo(BigDecimal.ZERO);
        });
        assertThat(resultat).anySatisfy(endring -> {
            assertThat(endring.getPeriode()).isEqualTo(Periode.of(endringFom, null));
            assertThat(endring.getMånedsbeløp()).isEqualByComparingTo(BigDecimal.TEN);
        });
    }
}
