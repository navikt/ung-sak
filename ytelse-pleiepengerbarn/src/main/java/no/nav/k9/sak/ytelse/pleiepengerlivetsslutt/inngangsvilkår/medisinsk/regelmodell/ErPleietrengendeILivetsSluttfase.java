package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErPleietrengendeILivetsSluttfase.ID)
public class ErPleietrengendeILivetsSluttfase extends LeafSpecification<MedisinskvilkårGrunnlag> {

    static final String ID = "PSB_VK_9.10.1";

    ErPleietrengendeILivetsSluttfase() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskvilkårGrunnlag grunnlag) {
        if (grunnlag.getRelevantVurderingLivetsSlutt().stream().anyMatch(it -> it.overlaps(grunnlag.getInterval()))) {
            return ja();
        }
        return nei();
    }

}
