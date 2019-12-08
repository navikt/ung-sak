package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ytelse.svp;

import java.math.BigDecimal;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettAndelLikBruttoBG.ID)
public class FastsettAndelLikBruttoBG extends LeafSpecification<BeregningsgrunnlagPeriode> {

    static final String ID = "FP_BR 29.6.2";
    static final String BESKRIVELSE = "Fastsett andel lik brutto bg";

    public FastsettAndelLikBruttoBG() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsgrunnlagPeriode grunnlag) {

        for (BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus : grunnlag.getBeregningsgrunnlagPrStatusSomSkalBrukes()) {
            if (AktivitetStatus.erArbeidstaker(beregningsgrunnlagPrStatus.getAktivitetStatus())) {
                for (BeregningsgrunnlagPrArbeidsforhold af : beregningsgrunnlagPrStatus.getArbeidsforholdSomSkalBrukes()) {
                    BigDecimal bruttoInkludertNaturalytelsePrÅr = af.getBruttoInkludertNaturalytelsePrÅr().orElse(BigDecimal.ZERO);
                    BeregningsgrunnlagPrArbeidsforhold.builder(af)
                        .medAndelsmessigFørGraderingPrAar(bruttoInkludertNaturalytelsePrÅr)
                        .build();
                }
            } else {
                BigDecimal avkortetPrStatus = beregningsgrunnlagPrStatus.getBruttoPrÅr();
                BeregningsgrunnlagPrStatus.builder(beregningsgrunnlagPrStatus)
                    .medAndelsmessigFørGraderingPrAar(avkortetPrStatus)
                    .build();
            }
        }
        return ja();

    }

}
