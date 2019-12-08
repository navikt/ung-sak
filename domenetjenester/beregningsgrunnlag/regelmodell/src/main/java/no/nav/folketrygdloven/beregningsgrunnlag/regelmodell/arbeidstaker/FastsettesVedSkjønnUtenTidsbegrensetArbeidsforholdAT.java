package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.arbeidstaker;

import java.math.BigDecimal;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.IkkeBeregnet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SammenligningsGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

@RuleDocumentation(FastsettesVedSkjønnUtenTidsbegrensetArbeidsforholdAT.ID)
class FastsettesVedSkjønnUtenTidsbegrensetArbeidsforholdAT extends IkkeBeregnet {

    static final String ID = "5038";
    static final String BESKRIVELSE = "Avvik for AT er > 25%, beregningsgrunnlag fastsettes ved skjønn";
    private static final RuleReasonRef AVVIK_MER_ENN_25_PROSENT = new RuleReasonRefImpl(ID, BESKRIVELSE);

    FastsettesVedSkjønnUtenTidsbegrensetArbeidsforholdAT() {
        super(AVVIK_MER_ENN_25_PROSENT);
    }

    @Override
    public Evaluation evaluate(BeregningsgrunnlagPeriode grunnlag) {
        SammenligningsGrunnlag sg = grunnlag.getSammenligningsgrunnlagPrStatus(AktivitetStatus.AT);
        BigDecimal avvikProsent = sg.getAvvikProsent();
        return nei(new RuleReasonRefImpl(ID, String.valueOf(avvikProsent)));
    }
}
