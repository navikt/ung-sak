package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fordel;

import static java.util.Collections.emptyList;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.node.SingleEvaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkHarRefusjonSomOverstigerBeregningsgrunnlag.ID)
class SjekkHarRefusjonSomOverstigerBeregningsgrunnlag extends LeafSpecification<BeregningsgrunnlagPeriode> {

    static final String ID = "FP_BR 22.3.1";
    static final String BESKRIVELSE = "Har arbeidstaker som søker refusjon som overstiger beregningsgrunnlag?";

    SjekkHarRefusjonSomOverstigerBeregningsgrunnlag() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsgrunnlagPeriode grunnlag) {
        BeregningsgrunnlagPrStatus atfl = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL);
        List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforholdSomHarRefusjonStørreEnnBG = atfl == null ? emptyList() : atfl
            .getArbeidsforholdIkkeFrilans().stream()
            .filter(af -> af.getBruttoInkludertNaturalytelsePrÅr().isPresent())
            .filter(this::harRefusjonskravStørreEnnBg).collect(Collectors.toList());
        SingleEvaluation resultat = arbeidsforholdSomHarRefusjonStørreEnnBG.isEmpty() ? nei() : ja();
        for (BeregningsgrunnlagPrArbeidsforhold arbeidsforhold : arbeidsforholdSomHarRefusjonStørreEnnBG) {
            resultat.setEvaluationProperty("refusjonPrÅr." + arbeidsforhold.getArbeidsgiverId(), arbeidsforhold.getRefusjonskravPrÅr().orElse(BigDecimal.ZERO));
            resultat.setEvaluationProperty("bruttoPrÅr." + arbeidsforhold.getArbeidsgiverId(), arbeidsforhold.getBruttoInkludertNaturalytelsePrÅr());
        }
        return resultat;
    }

    private boolean harRefusjonskravStørreEnnBg(BeregningsgrunnlagPrArbeidsforhold andel) {
        BigDecimal refusjonskrav = andel.getRefusjonskravPrÅr().orElse(BigDecimal.ZERO);
        return refusjonskrav.compareTo(andel.getBruttoPrÅr()) > 0;
    }
}
