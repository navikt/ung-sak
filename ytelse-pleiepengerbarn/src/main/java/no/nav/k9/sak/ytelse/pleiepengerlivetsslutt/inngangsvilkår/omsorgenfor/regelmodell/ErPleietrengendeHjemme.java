package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErPleietrengendeHjemme.ID)
public class ErPleietrengendeHjemme extends LeafSpecification<PleiesHjemmeVilkårGrunnlag> {

    static final String ID = "PSB_VK_9.10.1";

    ErPleietrengendeHjemme() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(PleiesHjemmeVilkårGrunnlag grunnlag) {
        if (grunnlag.getErPleietIHjemmet() != null && grunnlag.getErPleietIHjemmet()) {
            return ja();
        }

        return nei();
    }
}
