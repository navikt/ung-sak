package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = OmsorgenForVilkår.ID, specificationReference = "")
public class OmsorgenForVilkår implements RuleService<OmsorgenForVilkårGrunnlag> {

    public static final String ID = "PSB_VK 9.10";

    @Override
    public Evaluation evaluer(OmsorgenForVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OmsorgenForVilkårGrunnlag> getSpecification() {
        Ruleset<OmsorgenForVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(HarSøkerOmsorgenForPleietrengende.ID, "Har søker omsorgen for brukeren.")
            .hvis(new HarSøkerOmsorgenForPleietrengende(), new Oppfylt())
            .ellers(new IkkeOppfylt(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason()));
    }
}
