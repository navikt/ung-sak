package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = NødvendighetVilkår.ID, specificationReference = "")
public class NødvendighetVilkår implements RuleService<NødvendighetVilkårGrunnlag> {

    public static final String ID = "OLP_VK 9.14.1";

    @Override
    public Evaluation evaluer(NødvendighetVilkårGrunnlag grunnlag) {
        final NødvendighetMellomregningData mellomregningData = new NødvendighetMellomregningData(grunnlag);

        return getSpecification().evaluate(mellomregningData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<NødvendighetMellomregningData> getSpecification() {
        Ruleset<NødvendighetMellomregningData> rs = new Ruleset<>();
        return rs.hvisRegel(ErNødvendigOpplæring.ID, "Hvis opplæringen er vurdert som nødvendig...")
            .hvis(new ErNødvendigOpplæring(), new Oppfylt())
            .ellers(new IkkeOppfylt(NødvendighetVilkårAvslagsårsaker.IKKE_NØDVENDIG.toRuleReason()));
    }
}
