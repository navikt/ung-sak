package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.util.List;

import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;

public class BeregningsgrunnlagPeriodeEndring {

    private final List<BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer;
    private final List<NyttInntektsforholdEndring> nyttInntektsforholdEndringer;

    private final Periode periode;


    public BeregningsgrunnlagPeriodeEndring(List<BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer, List<NyttInntektsforholdEndring> nyttInntektsforholdEndringer, Periode periode) {
        this.beregningsgrunnlagPrStatusOgAndelEndringer = beregningsgrunnlagPrStatusOgAndelEndringer;
        this.nyttInntektsforholdEndringer = nyttInntektsforholdEndringer;
        this.periode = periode;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelEndring> getBeregningsgrunnlagPrStatusOgAndelEndringer() {
        return beregningsgrunnlagPrStatusOgAndelEndringer;
    }

    public List<NyttInntektsforholdEndring> getNyttInntektsforholdEndringer() {
        return nyttInntektsforholdEndringer;
    }

    public Periode getPeriode() {
        return periode;
    }
}
