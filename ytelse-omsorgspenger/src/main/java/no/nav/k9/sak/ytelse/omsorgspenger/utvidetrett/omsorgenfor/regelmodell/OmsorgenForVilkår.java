package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeVurderbar;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = OmsorgenForVilkår.ID, specificationReference = "")
public class OmsorgenForVilkår implements RuleService<OmsorgenForVilkårGrunnlag> {

    public static final String ID = "OMP_VK 9.5";

    @Override
    public Evaluation evaluer(OmsorgenForVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OmsorgenForVilkårGrunnlag> getSpecification() {
        Ruleset<OmsorgenForVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(HarSøkerOmsorgenForEgetBarn.ID, "Har søker omsorgen for barnet.")
            .hvis(new HarSøkerOmsorgenForEgetBarn(), new Oppfylt())
            .ellers(new IkkeVurderbar(OmsorgenForKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR.toRuleReason()));
    }

}
