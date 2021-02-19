package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

/**
 * Dersom bruker har oppgitt at han/hun har et aktivt frilans arbeidsforhold, anses opptjeningsvilkåret som oppfylt. *
 */
@RuleDocumentation(value = "FP_VK_23.2.6")
public class SjekkOppgittFrilansArbeidsforhold extends LeafSpecification<MellomregningOpptjeningsvilkårData> {
    public static final String ID = SjekkOppgittFrilansArbeidsforhold.class.getSimpleName();

    public SjekkOppgittFrilansArbeidsforhold() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MellomregningOpptjeningsvilkårData data) {
        if (data.getGrunnlag().brukerHarOppgittAktivtFrilansArbeidsforhold()) {
            return ja();
        } else {
            return nei();
        }
    }

}
