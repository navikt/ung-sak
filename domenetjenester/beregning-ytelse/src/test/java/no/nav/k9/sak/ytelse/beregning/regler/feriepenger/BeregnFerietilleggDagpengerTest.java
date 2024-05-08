package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.DagpengerKilde;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.DagpengerPeriode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BeregnFerietilleggDagpengerTest {
    private static final LocalDate STP = LocalDate.of(2023,6,1);

    @Test
    void skal_ikke_gi_ferietillegg_når_under_8_uker_dagpenger() {
        var tyPeriode = lagTilkjentYtelseDagpenger(STP, etter(10), 900L);

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(Collections.emptyList())
            .medBeregningsresultatPerioder(List.of(tyPeriode))
            .build();
        new BeregnFerietilleggDagpenger().evaluate(regelmodell);

        assertThat(regelmodell).isNotNull();

        var feriepengerP1 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 0);

        assertThat(feriepengerP1).isEmpty();
    }

    @Test
    void skal_gi_ferietillegg_når_over_8_uker_dagpenger() {
        var tyPeriode = lagTilkjentYtelseDagpenger(STP, etter(100), 900L);

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(Collections.emptyList())
            .medBeregningsresultatPerioder(List.of(tyPeriode))
            .build();
        new BeregnFerietilleggDagpenger().evaluate(regelmodell);

        assertThat(regelmodell).isNotNull();

        var feriepengerP1 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 0);

        assertThat(feriepengerP1).hasSize(1);
        assertThat(feriepengerP1.getFirst().getOpptjeningÅr()).isEqualTo(LocalDate.of(2023,1,1));
        assertThat(feriepengerP1.getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(6156));
    }

    @Test
    void skal_ikke_gi_feriepenger_for_periode_før_regelendring() {
        var tyPeriode = lagTilkjentYtelseDagpenger(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 12, 31), 900L);

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(Collections.emptyList())
            .medBeregningsresultatPerioder(List.of(tyPeriode))
            .build();
        new BeregnFerietilleggDagpenger().evaluate(regelmodell);

        assertThat(regelmodell).isNotNull();

        var feriepengerP1 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 0);

        assertThat(feriepengerP1).isEmpty();
    }

    @Test
    void skal_ikke_gi_ferietillegg_når_over_8_uker_med_dagpenger_over_flere_år_fra_andre_kilder() {
        var tyPeriode = lagTilkjentYtelseDagpenger(LocalDate.of(2023,11,15), LocalDate.of(2024,2,20), 900L);

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(List.of())
            .medBeregningsresultatPerioder(List.of(tyPeriode))
            .build();
        new BeregnFerietilleggDagpenger().evaluate(regelmodell);

        assertThat(regelmodell).isNotNull();

        var feriepengerP1 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 0);

        assertThat(feriepengerP1).isEmpty();
    }

    @Test
    void skal_gi_ferietillegg_når_over_8_med_dagpenger_fra_andre_kilder() {
        var tyPeriode = lagTilkjentYtelseDagpenger(STP, etter(10), 900L);
        var dp1 = lagDPPeriode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));
        var dp2 = lagDPPeriode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 30));

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(List.of(dp1, dp2))
            .medBeregningsresultatPerioder(List.of(tyPeriode))
            .build();
        new BeregnFerietilleggDagpenger().evaluate(regelmodell);

        assertThat(regelmodell).isNotNull();

        var feriepengerP1 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 0);

        assertThat(feriepengerP1).hasSize(1);
        assertThat(feriepengerP1.getFirst().getOpptjeningÅr()).isEqualTo(LocalDate.of(2023,1,1));
        assertThat(feriepengerP1.getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(599));
    }

    @Test
    void skal_gi_ferietillegg_i_kun_første_år_når_over_8_uker_i_ett_av_to_år() {
        var tyPeriode = lagTilkjentYtelseDagpenger(LocalDate.of(2023,9,1), LocalDate.of(2024,1, 31), 900L);

        var dp1 = lagDPPeriode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));
        var dp2 = lagDPPeriode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 30));

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(List.of(dp1, dp2))
            .medBeregningsresultatPerioder(List.of(tyPeriode))
            .build();
        new BeregnFerietilleggDagpenger().evaluate(regelmodell);

        assertThat(regelmodell).isNotNull();

        var feriepengerP1 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 0);

        assertThat(feriepengerP1).hasSize(1);
        assertThat(feriepengerP1.getFirst().getOpptjeningÅr()).isEqualTo(LocalDate.of(2023,1,1));
        assertThat(feriepengerP1.getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(7353));
    }


    @Test
    void skal_gi_ferietillegg_for_flere_perioder_med_totalt_avrundet_beløp_under_1_krone() {
        var tyPeriode = lagTilkjentYtelseDagpenger(STP, STP, 101L);
        var tyPeriode2 = lagTilkjentYtelseDagpenger(etter(1), etter(1), 102L);
        var tyPeriode3 = lagTilkjentYtelseDagpenger(etter(2), etter(3), 0L);
        var tyPeriode4 = lagTilkjentYtelseDagpenger(etter(4), etter(4), 101L);

        var dp1 = lagDPPeriode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));
        var dp2 = lagDPPeriode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 30));

        var regelmodell = BeregningsresultatFeriepengerRegelModell.builder()
            .medPerioderMedDagpenger(List.of(dp1, dp2))
            .medBeregningsresultatPerioder(List.of(tyPeriode, tyPeriode2, tyPeriode3, tyPeriode4))
            .build();

        var evaluation = new BeregnFerietilleggDagpenger().evaluate(regelmodell);
        var sporing = evaluation.getEvaluationProperties();

        assertThat(regelmodell).isNotNull();

        var feriepengerP1 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 0);
        var feriepengerP2 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 1);
        var feriepengerP3 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 2);
        var feriepengerP4 = feriepengerDagpengerFraPeriodeIndex(regelmodell, 3);

        assertThat(feriepengerP1).hasSize(1);
        assertThat(feriepengerP1.getFirst().getOpptjeningÅr()).isEqualTo(LocalDate.of(2023,1,1));
        assertThat(feriepengerP1.getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(10)); // 9.595 -> 0.405
        assertThat(((BigDecimal) sporing.get("Feriepenger.avrunding.BRUKER.DP i perioden [2023-06-01, 2023-06-01]")).compareTo(BigDecimal.valueOf(0.405))).isEqualTo(0);

        assertThat(feriepengerP2).hasSize(1);
        assertThat(feriepengerP2.getFirst().getOpptjeningÅr()).isEqualTo(LocalDate.of(2023,1,1));
        assertThat(feriepengerP2.getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(9)); // 9.69 -> 9.69 - 0.405 = 9.285 -> 9 -> -0.69 -> total feil = -0.285
        assertThat(((BigDecimal) sporing.get("Feriepenger.avrunding.BRUKER.DP i perioden [2023-06-02, 2023-06-02]")).compareTo(BigDecimal.valueOf(-0.690))).isEqualTo(0);

        assertThat(feriepengerP3).hasSize(1);
        assertThat(feriepengerP3.getFirst().getOpptjeningÅr()).isEqualTo(LocalDate.of(2023,1,1));
        assertThat(feriepengerP3.getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(((BigDecimal) sporing.get("Feriepenger.BRUKER.DP i perioden [2023-06-03, 2023-06-04]")).compareTo(BigDecimal.ZERO)).isEqualTo(0);

        assertThat(feriepengerP4).hasSize(1);
        assertThat(feriepengerP4.getFirst().getOpptjeningÅr()).isEqualTo(LocalDate.of(2023,1,1));
        assertThat(feriepengerP4.getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(10)); // 9.595 -> 0.405
        assertThat(((BigDecimal) sporing.get("Feriepenger.avrunding.BRUKER.DP i perioden [2023-06-05, 2023-06-05]")).compareTo(BigDecimal.valueOf(0.405))).isEqualTo(0);
    }

    private DagpengerPeriode lagDPPeriode(LocalDate fom, LocalDate tom) {
        return new DagpengerPeriode(DagpengerKilde.MELDEKORT, fom, tom);
    }

    private static List<BeregningsresultatFeriepengerPrÅr> feriepengerDagpengerFraPeriodeIndex(BeregningsresultatFeriepengerRegelModell regelmodell, int index) {
        return regelmodell.getBeregningsresultatPerioder()
            .get(index)
            .getBeregningsresultatAndelList().stream()
            .filter(a -> a.getInntektskategori().equals(Inntektskategori.DAGPENGER))
            .flatMap(a -> a.getBeregningsresultatFeriepengerPrÅrListe().stream())
            .toList();
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
