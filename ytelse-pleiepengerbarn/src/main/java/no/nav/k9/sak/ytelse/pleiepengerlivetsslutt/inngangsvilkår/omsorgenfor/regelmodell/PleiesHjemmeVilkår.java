package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = PleiesHjemmeVilkår.ID, specificationReference = "")
public class PleiesHjemmeVilkår implements RuleService<PleiesHjemmeVilkårGrunnlag> {

    public static final String ID = "PLS_VK 9.13";

    static final RuleReasonRef MANGLENDE_DOKUMENTASJON = new RuleReasonRefImpl("1019", "Manglende dokumentasjon");

    @Override
    public Evaluation evaluer(PleiesHjemmeVilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<PleiesHjemmeVilkårGrunnlag> getSpecification() {
        Ruleset<PleiesHjemmeVilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(PleiesPleietrengendeHjemme.ID, "Er pleietrengende hjemme?")
            .hvis(new PleiesPleietrengendeHjemme(), new Oppfylt())
            .ellers(new IkkeOppfylt(MANGLENDE_DOKUMENTASJON));
    }
}
