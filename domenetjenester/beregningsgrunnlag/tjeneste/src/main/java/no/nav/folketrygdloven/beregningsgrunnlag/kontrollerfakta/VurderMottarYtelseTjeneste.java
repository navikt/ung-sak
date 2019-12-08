package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public class VurderMottarYtelseTjeneste {

    private VurderMottarYtelseTjeneste() {
        // Skjul
    }

    public static boolean skalVurdereMottattYtelse(BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        boolean erFrilanser = erFrilanser(beregningsgrunnlag);
        if (erFrilanser) {
            return true;
        }
        return !ArbeidstakerUtenInntektsmeldingTjeneste
            .finnArbeidstakerAndelerUtenInntektsmelding(beregningsgrunnlag, iayGrunnlag).isEmpty();
    }

    public static boolean erFrilanser(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(andel -> andel.getAktivitetStatus().erFrilanser());
    }

}
