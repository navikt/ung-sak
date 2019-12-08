package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fastsette;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.node.SingleEvaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettIkkeSøktForTil0.ID)
class FastsettIkkeSøktForTil0 extends LeafSpecification<BeregningsgrunnlagPeriode> {

    static final String ID = "FP_BR_29_1";
    static final String BESKRIVELSE = "Fastsett andeler ikke søkt for til 0";

    FastsettIkkeSøktForTil0() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsgrunnlagPeriode grunnlag) {

        Map<String, Object> resultater = new HashMap<>();
        grunnlag.getBeregningsgrunnlagPrStatusSomSkalBrukes().stream()
            .flatMap(bgs -> bgs.getArbeidsforhold().stream())
            .filter(af -> !af.getErSøktYtelseFor())
            .forEach(af -> {
                BeregningsgrunnlagPrArbeidsforhold.builder(af)
                    .medAvkortetBrukersAndelPrÅr(BigDecimal.ZERO)
                    .medAvkortetRefusjonPrÅr(BigDecimal.ZERO)
                    .medAvkortetPrÅr(BigDecimal.ZERO)
                    .medRedusertRefusjonPrÅr(BigDecimal.ZERO)
                    .medRedusertPrÅr(BigDecimal.ZERO)
                    .medRedusertBrukersAndelPrÅr(BigDecimal.ZERO)
                    .medMaksimalRefusjonPrÅr(BigDecimal.ZERO)
                    .build();
                resultater.put("brukersAndel." + af.getArbeidsgiverId(), BigDecimal.ZERO);
            });
        grunnlag.getBeregningsgrunnlagPrStatus().stream()
            .filter(bgps -> !bgps.erArbeidstakerEllerFrilanser())
            .filter(bgps -> !bgps.erSøktYtelseFor())
            .forEach(bgps -> {
                BeregningsgrunnlagPrStatus.builder(bgps)
                    .medAvkortetPrÅr(BigDecimal.ZERO)
                    .medRedusertPrÅr(BigDecimal.ZERO)
                    .build();
                resultater.put("avkortetPrÅr.status." + bgps.getAktivitetStatus().name(), BigDecimal.ZERO);
            });


        SingleEvaluation resultat = ja();
        resultat.setEvaluationProperties(resultater);
        return resultat;
    }
}
