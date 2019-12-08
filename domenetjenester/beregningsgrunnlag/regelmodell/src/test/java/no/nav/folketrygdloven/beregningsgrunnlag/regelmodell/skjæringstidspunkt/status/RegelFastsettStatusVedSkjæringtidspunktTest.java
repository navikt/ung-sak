package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regelmodell.AktivPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regelmodell.BeregningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.status.RegelFastsettStatusVedSkjæringstidspunkt;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;

public class RegelFastsettStatusVedSkjæringtidspunktTest {
    private static final String ORGNR = "7654";
    private static final Arbeidsforhold ARBEIDSFORHOLD =  Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR);
    private LocalDate skjæringstidspunktForBeregning;
    private AktivitetStatusModell regelmodell;

    @Before
    public void setup() {
        skjæringstidspunktForBeregning = LocalDate.of(2018, Month.JANUARY, 15);
        regelmodell = new AktivitetStatusModell();
        regelmodell.setSkjæringstidspunktForBeregning(skjæringstidspunktForBeregning);
    }

    @Test
    public void skalFastsetteStatusDPNårAktivitetErDagpengerMottaker(){
        // Arrange
        AktivPeriode aktivPeriode = AktivPeriode.forAndre(Aktivitet.DAGPENGEMOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusWeeks(4), skjæringstidspunktForBeregning.plusWeeks(2)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(1);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsOnly(AktivitetStatus.DP);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe().get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.DP);
    }

    @Test
    public void skalFastsetteStatusATFLNårAktivitetErArbeidsinntektOgSykepengerOpphørtToDagerFørSP(){
        // Arrange
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.ARBEIDSTAKERINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusWeeks(2), skjæringstidspunktForBeregning.plusWeeks(3)), ARBEIDSFORHOLD);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = AktivPeriode.forAndre(Aktivitet.SVANGERSKAPSPENGER_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusMonths(2), skjæringstidspunktForBeregning.minusDays(2)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(2);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsOnly(AktivitetStatus.ATFL);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        BeregningsgrunnlagPrStatus bgPrStatus = regelmodell.getBeregningsgrunnlagPrStatusListe().get(0);
        assertThat(bgPrStatus.getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(bgPrStatus.getArbeidsforholdList()).hasSize(1);
        assertThat(bgPrStatus.getArbeidsforholdList().get(0).getOrgnr()).isEqualTo(ORGNR);
    }

    @Test
    public void skalFastsetteStatusArbeidstakerNårAktivitetErArbeidsinntektOgSykepengerOpphørt1DagFørSP(){
        // Arrange
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.ARBEIDSTAKERINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusWeeks(2), skjæringstidspunktForBeregning.plusWeeks(3)), ARBEIDSFORHOLD);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = AktivPeriode.forAndre(Aktivitet.SVANGERSKAPSPENGER_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusMonths(2), skjæringstidspunktForBeregning.minusDays(1)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(2);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsOnly(AktivitetStatus.ATFL);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        BeregningsgrunnlagPrStatus bgPrStatus = regelmodell.getBeregningsgrunnlagPrStatusListe().get(0);
        assertThat(bgPrStatus.getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(bgPrStatus.getArbeidsforholdList()).hasSize(1);
        assertThat(bgPrStatus.getArbeidsforholdList().get(0).getOrgnr()).isEqualTo(ORGNR);
    }

    @Test
    public void skalFastsetteStatusKUNTYNårAktivitetErSvangerskapspenger(){
        // Arrange
        AktivPeriode aktivPeriode = AktivPeriode.forAndre(Aktivitet.SVANGERSKAPSPENGER_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusMonths(1), skjæringstidspunktForBeregning.plusWeeks(3)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(1);
        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsOnly(AktivitetStatus.KUN_YTELSE);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe().get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.BA);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe().get(0).getArbeidsforholdList()).isEmpty();
    }

    @Test
    public void skalFastsetteStatusKUNTYNårAktivitetErKunSykepengerPåSkjæringstidspunktet() {
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.ARBEIDSTAKERINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusMonths(12), skjæringstidspunktForBeregning.minusDays(2)), ARBEIDSFORHOLD);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        AktivPeriode aktivPeriode2 = new AktivPeriode(Aktivitet.FRILANSINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusMonths(12), skjæringstidspunktForBeregning.minusMonths(2)), Arbeidsforhold.frilansArbeidsforhold());
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode2);
        AktivPeriode aktivPeriode3 = AktivPeriode.forAndre(Aktivitet.SYKEPENGER_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusMonths(1), skjæringstidspunktForBeregning.plusWeeks(3)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode3);
        assertThat(regelmodell.getAktivePerioder()).hasSize(3);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsOnly(AktivitetStatus.KUN_YTELSE);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe().get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.BA);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe().get(0).getArbeidsforholdList()).isEmpty();
    }


    @Test
    public void skalFastsetteStatusATFLNårKombinasjonerAvAktivitetErArbeidsinntektOgSykepenger(){
        // Arrange
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.ARBEIDSTAKERINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusWeeks(2), skjæringstidspunktForBeregning.plusWeeks(3)), ARBEIDSFORHOLD);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = AktivPeriode.forAndre(Aktivitet.SYKEPENGER_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusMonths(1), skjæringstidspunktForBeregning.plusWeeks(1)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(2);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).hasSize(1);
        assertThat(regelmodell.getAktivitetStatuser()).containsOnly(AktivitetStatus.ATFL);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        BeregningsgrunnlagPrStatus bgPrStatus = regelmodell.getBeregningsgrunnlagPrStatusListe().get(0);
        assertThat(bgPrStatus.getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(bgPrStatus.getArbeidsforholdList()).hasSize(1);
        assertThat(bgPrStatus.getArbeidsforholdList().get(0).getOrgnr()).isEqualTo(ORGNR);
    }

    @Test
    public void skalFastsetteStatusAAPVedKombinasjonerAvTYOgAAP(){
        // Arrange
        AktivPeriode aktivPeriode = AktivPeriode.forAndre(Aktivitet.AAP_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusWeeks(2), skjæringstidspunktForBeregning.plusWeeks(3)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = AktivPeriode.forAndre(Aktivitet.FORELDREPENGER_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusMonths(1), skjæringstidspunktForBeregning.plusWeeks(1)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(2);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).hasSize(1);
        assertThat(regelmodell.getAktivitetStatuser()).containsExactlyInAnyOrder(AktivitetStatus.AAP);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        BeregningsgrunnlagPrStatus bgPrStatus = regelmodell.getBeregningsgrunnlagPrStatusListe().get(0);
        assertThat(bgPrStatus.getAktivitetStatus()).isEqualTo(AktivitetStatus.AAP);
    }

    @Test
    public void skalFastsetteStatusDPVedKombinasjonerAvTYOgDP(){
        // Arrange
        AktivPeriode aktivPeriode = AktivPeriode.forAndre(Aktivitet.DAGPENGEMOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusDays(1), skjæringstidspunktForBeregning.plusWeeks(3)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = AktivPeriode.forAndre(Aktivitet.PLEIEPENGER_MOTTAKER, Periode.of(skjæringstidspunktForBeregning.minusWeeks(2), skjæringstidspunktForBeregning.plusDays(3)));
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(2);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).hasSize(1);
        assertThat(regelmodell.getAktivitetStatuser()).containsExactlyInAnyOrder(AktivitetStatus.DP);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        BeregningsgrunnlagPrStatus bgPrStatus = regelmodell.getBeregningsgrunnlagPrStatusListe().get(0);
        assertThat(bgPrStatus.getAktivitetStatus()).isEqualTo(AktivitetStatus.DP);
    }

    @Test
    public void skalFastsetteStatusATFL_SNVedKombinasjonerAvAktivitetFrilanserOgNæringsinntekt(){
        // Arrange
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.FRILANSINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusDays(3), skjæringstidspunktForBeregning.plusWeeks(2)), Arbeidsforhold.frilansArbeidsforhold());
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = new AktivPeriode(Aktivitet.NÆRINGSINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusWeeks(1), skjæringstidspunktForBeregning.plusDays(3)), null);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(2);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsOnly(AktivitetStatus.ATFL_SN);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(2);
        List<BeregningsgrunnlagPrStatus> bgPrStatuser = regelmodell.getBeregningsgrunnlagPrStatusListe();
        assertThat(bgPrStatuser.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(bgPrStatuser.get(0).getArbeidsforholdList()).hasSize(1);
        assertThat(bgPrStatuser.get(1).getAktivitetStatus()).isEqualTo(AktivitetStatus.SN);
        assertThat(bgPrStatuser.get(1).getArbeidsforholdList()).isEmpty();
    }

    @Test
    public void skalFastsetteStatusATFL_SNogDPVedKombinasjonerAvAktivitetArbeidsinntektNæringsinntektogMilitær(){
        // Arrange
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.FRILANSINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusDays(3), skjæringstidspunktForBeregning.plusWeeks(2)), Arbeidsforhold.frilansArbeidsforhold());
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = new AktivPeriode(Aktivitet.NÆRINGSINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusWeeks(1), skjæringstidspunktForBeregning.plusDays(3)), null);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = new AktivPeriode(Aktivitet.MILITÆR_ELLER_SIVILTJENESTE, Periode.of(skjæringstidspunktForBeregning.minusWeeks(4), skjæringstidspunktForBeregning.plusDays(5)), null);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(3);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsExactlyInAnyOrder(AktivitetStatus.ATFL_SN);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(2);
        List<BeregningsgrunnlagPrStatus> bgPrStatuser = regelmodell.getBeregningsgrunnlagPrStatusListe();
        assertThat(bgPrStatuser.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(bgPrStatuser.get(0).getArbeidsforholdList()).hasSize(1);
        assertThat(bgPrStatuser.get(1).getAktivitetStatus()).isEqualTo(AktivitetStatus.SN);
        assertThat(bgPrStatuser.get(1).getArbeidsforholdList()).isEmpty();
    }

    @Test
    public void skalFastsetteStatusMSNårBareErMilitærPåStp(){
        // Arrange
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.MILITÆR_ELLER_SIVILTJENESTE, Periode.of(skjæringstidspunktForBeregning.minusMonths(4),
            skjæringstidspunktForBeregning.plusDays(5)), null);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(1);

        // Actautoaut
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsExactlyInAnyOrder(AktivitetStatus.MS);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        List<BeregningsgrunnlagPrStatus> bgPrStatuser = regelmodell.getBeregningsgrunnlagPrStatusListe();
        assertThat(bgPrStatuser.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.MS);
        assertThat(bgPrStatuser.get(0).getArbeidsforholdList()).isEmpty();
    }

    @Test
    public void skalFastsetteStatusATFLNårErBådeArbeidstakerOgMilitærPåStp(){
        // Arrange
        AktivPeriode aktivPeriode = new AktivPeriode(Aktivitet.ARBEIDSTAKERINNTEKT, Periode.of(skjæringstidspunktForBeregning.minusMonths(8), skjæringstidspunktForBeregning), ARBEIDSFORHOLD);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        aktivPeriode = new AktivPeriode(Aktivitet.MILITÆR_ELLER_SIVILTJENESTE, Periode.of(skjæringstidspunktForBeregning.minusMonths(4), skjæringstidspunktForBeregning), null);
        regelmodell.leggTilEllerOppdaterAktivPeriode(aktivPeriode);
        assertThat(regelmodell.getAktivePerioder()).hasSize(2);

        // Act
        Evaluation evaluation = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(regelmodell.getAktivitetStatuser()).containsExactlyInAnyOrder(AktivitetStatus.ATFL);
        assertThat(regelmodell.getBeregningsgrunnlagPrStatusListe()).hasSize(1);
        List<BeregningsgrunnlagPrStatus> bgPrStatuser = regelmodell.getBeregningsgrunnlagPrStatusListe();
        assertThat(bgPrStatuser.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(bgPrStatuser.get(0).getArbeidsforholdList()).hasSize(1);
    }

}
