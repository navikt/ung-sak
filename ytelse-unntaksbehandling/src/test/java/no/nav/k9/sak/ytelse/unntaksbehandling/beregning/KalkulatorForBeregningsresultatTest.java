package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

class KalkulatorForBeregningsresultatTest {

    @Test
    public void skal_summer_helg_for_omsorgspenger() {
        BeregningsresultatEntitet beregningsresultat = lagBeregningsresultatForEnUke(1000);
        KalkulatorForBeregningsresultat kalkulator = new KalkulatorForBeregningsresultat(FagsakYtelseType.OMSORGSPENGER);
        long totalsum = kalkulator.beregnTotalsum(beregningsresultat);
        Assertions.assertThat(totalsum).isEqualTo(7 * 1000L);
    }

    @Test
    public void skal_summere_ytelse_kun_i_ukedager_selv_om_periode_inkluderer_helg_for_pleiepenger() {
        BeregningsresultatEntitet beregningsresultat = lagBeregningsresultatForEnUke(1000);
        KalkulatorForBeregningsresultat kalkulator = new KalkulatorForBeregningsresultat(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        long totalsum = kalkulator.beregnTotalsum(beregningsresultat);
        Assertions.assertThat(totalsum).isEqualTo(5 * 1000L);
    }

    private BeregningsresultatEntitet lagBeregningsresultatForEnUke(int dagsats) {
        LocalDate idag = LocalDate.now();
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("foo").medRegelSporing("bar").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(idag, idag.plusDays(6))
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(dagsats)
            .medDagsatsFraBg(1)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .buildFor(beregningsresultatPeriode);
        return beregningsresultat;
    }

}
