package no.nav.k9.sak.inngangsvilkår;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.specification.LeafSpecification;

/** Brukes for å trigge manuell vurdering når automatisk regel ikke kan vurderes til innvilget/avslått */
public class IkkeVurderbar<T> extends LeafSpecification<T> {

    private final RuleReasonRef ruleReasonRef;

    public IkkeVurderbar(RuleReasonRef ruleReasonRef){
        super(ruleReasonRef.getReasonCode());
        this.ruleReasonRef = ruleReasonRef;
    }
    @Override
    public Evaluation evaluate(T grunnlag) {
        return kanIkkeVurdere(ruleReasonRef);
    }

    @Override
    public String beskrivelse() {
        return ruleReasonRef.getReasonTextTemplate();
    }

}
