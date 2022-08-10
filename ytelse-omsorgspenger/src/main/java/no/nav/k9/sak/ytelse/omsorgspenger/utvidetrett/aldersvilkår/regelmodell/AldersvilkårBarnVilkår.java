package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeVurderbar;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = AldersvilkårBarnVilkår.ID, specificationReference = "")
public class AldersvilkårBarnVilkår implements RuleService<AldersvilkårBarnVilkårGrunnlag> {

    public static final String ID = "OMP_VK 9.6.1";

    @Override
    public Evaluation evaluer(AldersvilkårBarnVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<AldersvilkårBarnVilkårGrunnlag> getSpecification() {
        Ruleset<AldersvilkårBarnVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(HarMinstEttBarnRiktiAlder.ID, "Har søker aleneomsorg.")
            .hvis(new HarMinstEttBarnRiktiAlder(), new Oppfylt())
            .ellers(new IkkeVurderbar<>(AldersvilkårBarnKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_ALDERSVILKÅR_BARN.toRuleReason()));
    }

}
