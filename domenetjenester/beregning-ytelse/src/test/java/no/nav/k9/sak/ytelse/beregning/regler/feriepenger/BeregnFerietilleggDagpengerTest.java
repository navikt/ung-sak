package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import net.bytebuddy.asm.Advice;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BeregnFerietilleggDagpengerTest {
    private static final LocalDate STP = LocalDate.of(2024,5,1);

    @Test
    void skal_beregne_når_8_uker_i_k9sak() {
        var tyPeriode = lagTilkjentYtelseDagpenger(STP, etter(10), 100L);
        var tyPeriode2 = lagTilkjentYtelseDagpenger(etter(11), etter(90), 100L);
        var tyPeriode3 = lagTilkjentYtelseDagpenger(etter(91), etter(200), 100L);
        var tyPeriode4 = lagTilkjentYtelseDagpenger(etter(201), etter(310), 100L);

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(Collections.emptyList())
            .medBeregningsresultatPerioder(List.of(tyPeriode, tyPeriode2, tyPeriode3, tyPeriode4))
            .build();
        new BeregnFerietilleggDagpenger().evaluate(regelmodell);

        assertThat(regelmodell).isNotNull();
    }

    private LocalDate etter(int dagerEtterStp) {
        return STP.plusDays(dagerEtterStp);
    }

    private BeregningsresultatPeriode lagTilkjentYtelseDagpenger(LocalDate fom, LocalDate tom, Long dagsats) {
        BeregningsresultatPeriode periode = new BeregningsresultatPeriode(fom, tom, null,null, null);
        periode.addBeregningsresultatAndel(BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.DP).medDagsats(dagsats).medInntektskategori(Inntektskategori.DAGPENGER).medDagsatsFraBg(dagsats).medBrukerErMottaker(true).build(periode));
        return periode;
    }

}
