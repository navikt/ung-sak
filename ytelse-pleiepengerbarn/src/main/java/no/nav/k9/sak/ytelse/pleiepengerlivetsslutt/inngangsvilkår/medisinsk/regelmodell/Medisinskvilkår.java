package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;
import no.nav.k9.sak.inngangsvilkår.Oppfylt;

@RuleDocumentation(value = Medisinskvilkår.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=173827808")
public class Medisinskvilkår implements RuleService<MedisinskvilkårGrunnlag> {

    // TODO PLS: Korrigere paragraf
    public static final String ID = "PSB_VK 9.10";

    static final RuleReasonRef MANGLENDE_DOKUMENTASJON = new RuleReasonRefImpl("1019", "Manglende dokumentasjon");

    @Override
    public Evaluation evaluer(MedisinskvilkårGrunnlag grunnlag, Object resultatStruktur) {
        final var evaluate = getSpecification().evaluate(grunnlag);
        return evaluate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<MedisinskvilkårGrunnlag> getSpecification() {
        Ruleset<MedisinskvilkårGrunnlag> rs = new Ruleset<>();
        return rs.hvisRegel(ErPleietrengendeILivetsSluttfase.ID, "Er pleietrengende i livets sluttfase?")
            .hvis(new ErPleietrengendeILivetsSluttfase(), new Oppfylt())
            .ellers(new IkkeOppfylt(MANGLENDE_DOKUMENTASJON));
    }
}
