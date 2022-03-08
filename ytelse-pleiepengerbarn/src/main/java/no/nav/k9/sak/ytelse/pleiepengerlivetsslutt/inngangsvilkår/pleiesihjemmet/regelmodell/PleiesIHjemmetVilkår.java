package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = PleiesIHjemmetVilkår.ID, specificationReference = "")
public class PleiesIHjemmetVilkår implements RuleService<PleiesIHjemmetVilkårGrunnlag> {

    public static final String ID = "PLS_VK 9.13";

    @Override
    public Evaluation evaluer(PleiesIHjemmetVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }


    @SuppressWarnings("unchecked")
    @Override
    public Specification<PleiesIHjemmetVilkårGrunnlag> getSpecification() {
        Ruleset<PleiesIHjemmetVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(ErPleietrengendePleietIHjemmet.ID, "Pleies pleietrengende i hjemmet?")
            .hvis(new ErPleietrengendePleietIHjemmet(), new Oppfylt())
            .ellers(new IkkeOppfylt(PleiesIHjemmetVilkårAvslagsårsaker.MANGLENDE_DOKUMENTASJON.toRuleReason()));
    }
}
