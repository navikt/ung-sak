package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapSplittetPeriodeFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.SplittetPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class MapSplittetPeriodeFraVLTilRegelTest {
    @Test
    public void ingenPeriodeårsak() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(6);
        BeregningsgrunnlagEntitet beregningsgrunnlag = mock(BeregningsgrunnlagEntitet.class);
        BeregningsgrunnlagPeriode bgPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(beregningsgrunnlag);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("abc");
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medAndelsnr(1L)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdRef(arbeidsforholdRef))
            .build(bgPeriode);

        // Act
        SplittetPeriode splittetPeriode = MapSplittetPeriodeFraVLTilRegel.map(bgPeriode);

        // Assert
        assertThat(splittetPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(splittetPeriode.getPeriode().getTom()).isEqualTo(tom);
        assertThat(splittetPeriode.getPeriodeÅrsaker()).isEmpty();
        assertThat(splittetPeriode.getEksisterendePeriodeAndeler()).hasSize(1);
        BeregningsgrunnlagPrArbeidsforhold bgPrArbeidsforhold = splittetPeriode.getEksisterendePeriodeAndeler().get(0);
        assertThat(bgPrArbeidsforhold.getAndelNr()).isEqualTo(1L);
        assertThat(bgPrArbeidsforhold.getArbeidsforhold().getOrgnr()).isEqualTo("abc");
        assertThat(bgPrArbeidsforhold.getArbeidsforhold().getArbeidsforholdId()).isNotNull();
        assertThat(bgPrArbeidsforhold.getArbeidsforhold().getArbeidsforholdId()).isEqualTo(arbeidsforholdRef.getReferanse());
        assertThat(splittetPeriode.getNyeAndeler()).isEmpty();
    }

    @Test
    public void enPeriodeårsak() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(6);
        BeregningsgrunnlagEntitet beregningsgrunnlag = mock(BeregningsgrunnlagEntitet.class);
        BeregningsgrunnlagPeriode bgPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .leggTilPeriodeÅrsak(PeriodeÅrsak.REFUSJON_OPPHØRER)
            .build(beregningsgrunnlag);

        // Act
        SplittetPeriode splittetPeriode = MapSplittetPeriodeFraVLTilRegel.map(bgPeriode);

        // Assert
        assertThat(splittetPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(splittetPeriode.getPeriode().getTom()).isEqualTo(tom);
        assertThat(splittetPeriode.getPeriodeÅrsaker()).hasSize(1);
        assertThat(splittetPeriode.getPeriodeÅrsaker().get(0)).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.REFUSJON_OPPHØRER);
    }

    @Test
    public void toPeriodeårsaker() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(6);
        BeregningsgrunnlagEntitet beregningsgrunnlag = mock(BeregningsgrunnlagEntitet.class);
        BeregningsgrunnlagPeriode bgPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .leggTilPeriodeÅrsak(PeriodeÅrsak.GRADERING)
            .leggTilPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .build(beregningsgrunnlag);

        // Act
        SplittetPeriode splittetPeriode = MapSplittetPeriodeFraVLTilRegel.map(bgPeriode);

        // Assert
        assertThat(splittetPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(splittetPeriode.getPeriode().getTom()).isEqualTo(tom);
        assertThat(splittetPeriode.getPeriodeÅrsaker()).hasSize(2);
        assertThat(splittetPeriode.getPeriodeÅrsaker().get(0)).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.GRADERING);
        assertThat(splittetPeriode.getPeriodeÅrsaker().get(1)).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
    }
}
