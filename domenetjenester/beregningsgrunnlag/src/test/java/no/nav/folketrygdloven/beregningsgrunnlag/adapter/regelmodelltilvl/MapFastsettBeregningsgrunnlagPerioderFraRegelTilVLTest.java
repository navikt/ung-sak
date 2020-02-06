package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.SplittetPeriode;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

public class MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, 1, 1);
    private static final BigDecimal RAPPORTERT_PR_ÅR = BigDecimal.valueOf(100000);
    private static final long AVVIK_PROMILLE = 20L;
    public static final Beløp GRUNNBELØP = new Beløp(BigDecimal.valueOf(10000));
    private static final List<FaktaOmBeregningTilfelle> FAKTA_OM_BEREGNING_TILFELLER = List.of(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);
    private static final String REGELINPUT = "Regelinput";
    private static final String REGELEVALUERING = "Regelevaluering";
    private static final BigDecimal BRUTTO_PR_ÅR = BigDecimal.valueOf(10000);
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.fra(AktørId.dummy());
    private static final BigDecimal NATURALYTELSE_TILKOMMET_PR_ÅR = BigDecimal.valueOf(2000);
    private static final BigDecimal BEREGNET_PR_ÅR = BigDecimal.valueOf(1000);
    private MapFastsettBeregningsgrunnlagPerioderFraRegelTilVL mapTilVlNaturalytelse = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse();
    private MapFastsettBeregningsgrunnlagPerioderFraRegelTilVL mapTilVlRefusjonOgGradering = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering();


    @Test
    public void skalMappeBeregningsgrunnlagUtenSplittNaturalytelse() {
        // Arrange
        BeregningsgrunnlagEntitet vlBeregningsgrunnlag = lagBeregningsgrunnlag();
        byggAktivitetStatus(vlBeregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = lagBeregningsgrunnlagPeriode(vlBeregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel andel = lagBeregnignsgrunnlagPrStatusOgAndel(periode);
        List<SplittetPeriode> splittetPerioder = List.of(SplittetPeriode.builder()
            .medPeriodeÅrsaker(Collections.emptyList())
            .medFørstePeriodeAndeler(List.of(mapTilBeregningsgrunnlagPrArbeidsforhold(andel)))
            .medPeriode(Periode.of(SKJÆRINGSTIDSPUNKT, null))
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBg = mapTilVlNaturalytelse.mapFraRegel(splittetPerioder, null, vlBeregningsgrunnlag);

        // Assert
        assertThat(nyttBg).isEqualToComparingFieldByFieldRecursively(vlBeregningsgrunnlag);
    }

    @Test
    public void skalMappeBeregningsgrunnlagUtenSplittRefusjonOgGradering() {
        // Arrange
        BeregningsgrunnlagEntitet vlBeregningsgrunnlag = lagBeregningsgrunnlag();
        byggAktivitetStatus(vlBeregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = lagBeregningsgrunnlagPeriode(vlBeregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel andel = lagBeregnignsgrunnlagPrStatusOgAndel(periode);
        List<SplittetPeriode> splittetPerioder = List.of(SplittetPeriode.builder()
            .medPeriodeÅrsaker(Collections.emptyList())
            .medFørstePeriodeAndeler(List.of(mapTilBeregningsgrunnlagPrArbeidsforhold(andel)))
            .medPeriode(Periode.of(SKJÆRINGSTIDSPUNKT, null))
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBg = mapTilVlRefusjonOgGradering.mapFraRegel(splittetPerioder, null, vlBeregningsgrunnlag);

        // Assert
        assertThat(nyttBg).isEqualToComparingFieldByFieldRecursively(vlBeregningsgrunnlag);
    }


    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        BeregningsgrunnlagEntitet vlBeregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medOverstyring(true)
            .medGrunnbeløp(GRUNNBELØP)
            .leggTilFaktaOmBeregningTilfeller(FAKTA_OM_BEREGNING_TILFELLER)
            .build();
        lagSammenligningsgrunnlag(vlBeregningsgrunnlag);
        return vlBeregningsgrunnlag;
    }

    private BeregningsgrunnlagPrStatusOgAndel lagBeregnignsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode periode) {
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBeregnetPrÅr(BEREGNET_PR_ÅR)
            .medOverstyrtPrÅr(BRUTTO_PR_ÅR)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER)
                .medNaturalytelseTilkommetPrÅr(NATURALYTELSE_TILKOMMET_PR_ÅR))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(periode);
    }

    private BeregningsgrunnlagPeriode lagBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        return BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
                .medRegelEvalueringForeslå(REGELINPUT, REGELEVALUERING)
                .medBruttoPrÅr(BRUTTO_PR_ÅR)
                .build(vlBeregningsgrunnlag);
    }

    private BeregningsgrunnlagPrArbeidsforhold mapTilBeregningsgrunnlagPrArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel) {
        return BeregningsgrunnlagPrArbeidsforhold.builder()
            .medAndelNr(andel.getAndelsnr())
            .medArbeidsforhold(Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(ARBEIDSGIVER.getIdentifikator())).build();
    }

    private void byggAktivitetStatus(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build(vlBeregningsgrunnlag);
    }

    private void lagSammenligningsgrunnlag(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(SKJÆRINGSTIDSPUNKT.minusMonths(3), SKJÆRINGSTIDSPUNKT)
            .medRapportertPrÅr(RAPPORTERT_PR_ÅR)
            .medAvvikPromille(AVVIK_PROMILLE).build(vlBeregningsgrunnlag);
    }
}
