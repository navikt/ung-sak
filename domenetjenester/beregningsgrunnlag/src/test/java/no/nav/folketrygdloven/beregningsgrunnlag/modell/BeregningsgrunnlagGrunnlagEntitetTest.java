package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;

public class BeregningsgrunnlagGrunnlagEntitetTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final ÅpenDatoIntervallEntitet PERIODE = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, Month.MARCH, 1), TIDENES_ENDE);

    @Test
    public void skal_returnere_register() {
        BeregningAktivitetEntitet beregningAktivitetSN = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();

        BeregningAktivitetEntitet beregningAktivitetAAP = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEIDSAVKLARING)
            .build();

        BeregningAktivitetAggregatEntitet registerAktiviteter = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(beregningAktivitetSN)
            .leggTilAktivitet(beregningAktivitetAAP)
            .build();

        BeregningsgrunnlagGrunnlagEntitet bgg = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(registerAktiviteter)
            .build(1L, BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        BeregningAktivitetAggregatEntitet resultat = bgg.getGjeldendeAktiviteter();

        // Assert
        assertThat(resultat.getSkjæringstidspunktOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(resultat.getBeregningAktiviteter()).hasSize(2);
        assertThat(resultat.getBeregningAktiviteter().get(0)).isEqualTo(beregningAktivitetSN);
        assertThat(resultat.getBeregningAktiviteter().get(1)).isEqualTo(beregningAktivitetAAP);
    }

    @Test
    public void skal_returnere_overstyringer() {
        BeregningAktivitetEntitet beregningAktivitetSN = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();

        BeregningAktivitetEntitet beregningAktivitetAAP = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEIDSAVKLARING)
            .build();

        BeregningAktivitetAggregatEntitet registerAktiviteter = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(beregningAktivitetSN)
            .leggTilAktivitet(beregningAktivitetAAP)
            .build();

        BeregningAktivitetOverstyringEntitet overstyring = lagOverstyringForBA(beregningAktivitetAAP);
        BeregningAktivitetOverstyringerEntitet overstyringerEntitet = BeregningAktivitetOverstyringerEntitet.builder()
            .leggTilOverstyring(overstyring)
            .build();
        BeregningsgrunnlagGrunnlagEntitet bgg = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(registerAktiviteter)
            .medOverstyring(overstyringerEntitet)
            .build(1L, BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        BeregningAktivitetAggregatEntitet resultat = bgg.getGjeldendeAktiviteter();

        // Assert
        assertThat(resultat.getSkjæringstidspunktOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(resultat.getBeregningAktiviteter()).hasSize(1);
        assertThat(resultat.getBeregningAktiviteter().get(0)).isEqualTo(beregningAktivitetSN);
    }

    @Test
    public void skal_returnere_overstyringer_når_saksbehandlet_finnes() {
        BeregningAktivitetEntitet beregningAktivitetSN = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();

        BeregningAktivitetEntitet beregningAktivitetAAP = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEIDSAVKLARING)
            .build();

        BeregningAktivitetAggregatEntitet registerAktiviteter = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(beregningAktivitetSN)
            .leggTilAktivitet(beregningAktivitetAAP)
            .build();

        BeregningAktivitetOverstyringEntitet overstyring = lagOverstyringForBA(beregningAktivitetAAP);
        BeregningAktivitetOverstyringerEntitet overstyringerEntitet = BeregningAktivitetOverstyringerEntitet.builder()
            .leggTilOverstyring(overstyring)
            .build();
        BeregningsgrunnlagGrunnlagEntitet bgg = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(registerAktiviteter)
            .medOverstyring(overstyringerEntitet)
            .medSaksbehandletAktiviteter(registerAktiviteter)
            .build(1L, BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        BeregningAktivitetAggregatEntitet resultat = bgg.getGjeldendeAktiviteter();

        // Assert
        assertThat(resultat.getSkjæringstidspunktOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(resultat.getBeregningAktiviteter()).hasSize(1);
        assertThat(resultat.getBeregningAktiviteter().get(0)).isEqualTo(beregningAktivitetSN);
    }

    private BeregningAktivitetOverstyringEntitet lagOverstyringForBA(BeregningAktivitetEntitet beregningAktivitet) {
        return BeregningAktivitetOverstyringEntitet.builder()
            .medOpptjeningAktivitetType(beregningAktivitet.getOpptjeningAktivitetType())
            .medPeriode(beregningAktivitet.getPeriode())
            .medArbeidsgiver(beregningAktivitet.getArbeidsgiver())
            .medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef())
            .medHandling(BeregningAktivitetHandlingType.IKKE_BENYTT)
            .build();
    }
}
