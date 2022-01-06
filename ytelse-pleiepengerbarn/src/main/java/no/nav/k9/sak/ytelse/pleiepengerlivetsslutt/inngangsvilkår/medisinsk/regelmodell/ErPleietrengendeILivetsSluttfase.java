package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErPleietrengendeILivetsSluttfase.ID)
public class ErPleietrengendeILivetsSluttfase extends LeafSpecification<MedisinskMellomregningData> {

    public static final String ID = "PLS_VK_9.16.1";

    public ErPleietrengendeILivetsSluttfase() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {
        final var grunnlag = mellomregning.getGrunnlag();

        if (grunnlag.getRelevantVurderingLivetsSlutt().stream().anyMatch(it -> it.overlaps(grunnlag.getInterval()))) {
            return ja();
        }
        return nei();
    }

}
