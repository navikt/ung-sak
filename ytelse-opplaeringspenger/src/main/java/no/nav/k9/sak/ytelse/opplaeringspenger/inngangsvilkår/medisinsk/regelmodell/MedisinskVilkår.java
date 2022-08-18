package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = MedisinskVilkår.ID, specificationReference = "")
public class MedisinskVilkår implements RuleService<MedisinskVilkårGrunnlag> {

    public static final String ID = "OLP_VK 9.16";

    @Override
    public Evaluation evaluer(MedisinskVilkårGrunnlag grunnlag, Object resultatStruktur) {
        final var mellomregningData = new MedisinskMellomregningData(grunnlag);

        final var evaluate = getSpecification().evaluate(mellomregningData);

        mellomregningData.oppdaterResultat((MedisinskVilkårResultat) resultatStruktur);

        return evaluate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<MedisinskMellomregningData> getSpecification() {
        Ruleset<MedisinskMellomregningData> rs = new Ruleset<>();
        return rs.hvisRegel(ErLangvarigSyk.ID, "TODO")
            .hvis(new ErLangvarigSyk(), new Oppfylt())
            .ellers(new IkkeOppfylt(MedisinskeVilkårAvslagsårsaker.NOE.toRuleReason()));

    }
}
