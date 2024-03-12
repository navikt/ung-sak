package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(value = "FP_VK_23.3")
public class HarPeriodeMedKunAAPogSN extends LeafSpecification<MellomregningOpptjeningsvilkårData> {
    public static final String ID = HarPeriodeMedKunAAPogSN.class.getSimpleName();

    public HarPeriodeMedKunAAPogSN() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MellomregningOpptjeningsvilkårData data) {
        if (data.getPerioderMedKunAAPogSN().isEmpty()) {
            return nei();
        } else {
            return ja();
        }
    }

}
