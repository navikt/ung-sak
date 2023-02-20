package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeVurderbar;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = AleneomsorgVilkår.ID, specificationReference = "")
public class AleneomsorgVilkår implements RuleService<AleneomsorgVilkårGrunnlag> {

    public static final String ID = "OMP_VK 9.6.1";

    @Override
    public Evaluation evaluer(AleneomsorgVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<AleneomsorgVilkårGrunnlag> getSpecification() {
        Ruleset<AleneomsorgVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(HarAleneomsorgForBarnet.ID, "Har søker aleneomsorg.")
            .hvis(new HarAleneomsorgForBarnet(), new Oppfylt())
            .ellers(new IkkeVurderbar<>(AleneomsorgKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR.toRuleReason()));
    }

}
