package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Medisinskvilkår;

@RuleDocumentation(value = Medisinskvilkår.ID, specificationReference = "")
public class OmsorgenForVilkår implements RuleService<OmsorgenForGrunnlag> {

    public static final String ID = "PSB_VK 9.10";

    @Override
    public Evaluation evaluer(OmsorgenForGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OmsorgenForGrunnlag> getSpecification() {
        Ruleset<OmsorgenForGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(HarSøkerOmsorgenForPleietrengende.ID, "Har søker omsorgen for brukeren.")
            .hvis(new HarSøkerOmsorgenForPleietrengende(), new Oppfylt())
            .ellers(new IkkeOppfylt(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason()));
    }
}
