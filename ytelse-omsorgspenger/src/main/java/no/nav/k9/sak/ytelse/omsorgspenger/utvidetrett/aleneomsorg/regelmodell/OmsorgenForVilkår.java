package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = OmsorgenForVilkår.ID, specificationReference = "")
public class OmsorgenForVilkår implements RuleService<AleneOmOmsorgenVilkårGrunnlag> {

    public static final String ID = "OMP_VK 9.5";

    @Override
    public Evaluation evaluer(AleneOmOmsorgenVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<AleneOmOmsorgenVilkårGrunnlag> getSpecification() {
        Ruleset<AleneOmOmsorgenVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(ErAleneOmOmsorgenForBarnet.ID, "Har søker omsorgen for barnet.")
            .hvis(new ErAleneOmOmsorgenForBarnet(), new Oppfylt())
            .ellers(new IkkeVurderbar(AleneOmOmsorgenKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR.toRuleReason()));
    }

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

}
