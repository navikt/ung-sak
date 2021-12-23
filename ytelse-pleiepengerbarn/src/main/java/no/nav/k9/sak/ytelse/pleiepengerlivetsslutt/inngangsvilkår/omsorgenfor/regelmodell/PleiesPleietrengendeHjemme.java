package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(PleiesPleietrengendeHjemme.ID)
public class PleiesPleietrengendeHjemme extends LeafSpecification<PleiesHjemmeVilkårGrunnlag> {

    static final String ID = "PLS_VK_9.13.1";

    PleiesPleietrengendeHjemme() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(PleiesHjemmeVilkårGrunnlag grunnlag) {
        if (grunnlag.getPleiesHjemme() != null && grunnlag.getPleiesHjemme()) {
            return ja();
        }

        return nei();
    }
}
