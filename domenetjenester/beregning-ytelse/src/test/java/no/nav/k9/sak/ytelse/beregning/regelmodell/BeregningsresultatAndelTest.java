package no.nav.k9.sak.ytelse.beregning.regelmodell;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;

class BeregningsresultatAndelTest {

    @Test
    void skal_si_at_andel_bare_skal_gi_feriepenger_når_inntektskategori_arbeidstaker_eller_sjømann() {
        for (Inntektskategori inntektskategori : Inntektskategori.values()) {
            boolean erArbeidstakerEllerSjømannn = inntektskategori == Inntektskategori.ARBEIDSTAKER || inntektskategori == Inntektskategori.SJØMANN;
            assertThat(lagAndel(1, inntektskategori).girRettTilFeriepenger()).isEqualTo(erArbeidstakerEllerSjømannn);
        }
    }

    @Test
    void skal_si_at_andel_ikke_kan_gi_feriepenger_når_dagsats_0() {
        assertThat(lagAndel(0, Inntektskategori.ARBEIDSTAKER).girRettTilFeriepenger()).isFalse();
        assertThat(lagAndel(0, Inntektskategori.SJØMANN).girRettTilFeriepenger()).isFalse();
    }

    private BeregningsresultatAndel lagAndel(int dagsats, Inntektskategori inntektskategori){
        BeregningsresultatPeriode periode = new BeregningsresultatPeriode(LocalDate.now(), LocalDate.now(), null, null,null, null, null);
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats((long)dagsats)
            .medDagsatsFraBg(0L)
            .medInntektskategori(inntektskategori)
            .medUtbetalingssgrad(BigDecimal.valueOf(100))
            .build(periode);
    }


}
