package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.FastsettMånedsinntektUtenInntektsmeldingTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

public class FastsettMånedsinntektUtenInntektsmeldingTilfelleUtlederTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, 1, 1);
    public static final String ORGNR = "21348714121";
    private FastsettMånedsinntektUtenInntektsmeldingTilfelleUtleder utleder = new FastsettMånedsinntektUtenInntektsmeldingTilfelleUtleder();

    @Test
    public void skal_gi_tilfelle_om_beregningsgrunnlag_har_andel_med_kunstig_arbeid() {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagGrunnlag(true);

        Optional<FaktaOmBeregningTilfelle> tilfelle = utleder.utled(grunnlag);
        assertThat(tilfelle.get()).isEqualTo(FaktaOmBeregningTilfelle.FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING);
    }

    @Test
    public void skal_ikkje_gi_tilfelle_om_beregningsgrunnlag_ikkje_har_andel_med_kunstig_arbeid() {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagGrunnlag(false);

        Optional<FaktaOmBeregningTilfelle> tilfelle = utleder.utled(grunnlag);
        assertThat(tilfelle).isNotPresent();
    }


    private BeregningsgrunnlagGrunnlagEntitet lagGrunnlag(boolean medKunstigArbeid) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null).build(bg);
        String orgnr = medKunstigArbeid ? OrgNummer.KUNSTIG_ORG : ORGNR;
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr)))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(bg).build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }
}
