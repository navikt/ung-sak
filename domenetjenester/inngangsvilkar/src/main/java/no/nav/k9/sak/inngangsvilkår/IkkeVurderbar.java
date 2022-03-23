package no.nav.k9.sak.inngangsvilkår;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class IkkeVurderbar extends LeafSpecification {

    private RuleReasonRef ruleReasonRef;

    public IkkeVurderbar(RuleReasonRef ruleReasonRef){
        super(ruleReasonRef.getReasonCode());
        this.ruleReasonRef = ruleReasonRef;
    }
    @Override
    public Evaluation evaluate(Object grunnlag) {
        return kanIkkeVurdere(ruleReasonRef);
    }

    @Override
    public String beskrivelse() {
        return ruleReasonRef.getReasonTextTemplate();
    }

}
