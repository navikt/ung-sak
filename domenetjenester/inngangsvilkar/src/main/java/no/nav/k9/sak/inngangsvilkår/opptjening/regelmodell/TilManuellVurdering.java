package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(value = "FP_VK_23.3")
public class TilManuellVurdering extends LeafSpecification<MellomregningOpptjeningsvilkårData> {
    public static final String ID = TilManuellVurdering.class.getSimpleName();

    static final String AKSONSPUNKT_KODE = "5089";
    static final RuleReasonRefImpl TIL_MANUELL_KONTROLL = new RuleReasonRefImpl(AKSONSPUNKT_KODE,
        "Minst deler av perioden har bare SN og AAP som aktiviteter. Da kreves manuell vurdering");

    public TilManuellVurdering() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MellomregningOpptjeningsvilkårData data) {
        return kanIkkeVurdere(TIL_MANUELL_KONTROLL);
    }

}
