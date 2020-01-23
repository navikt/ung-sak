package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErBehovForPleie.ID)
public class ErBehovForPleie extends LeafSpecification<MedisinskvilkårGrunnlag> {

    static final String ID = "FP_VK_2.12.1"; // FIXME (k9) Trenger en bedre referanse

    ErBehovForPleie() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskvilkårGrunnlag grunnlag) {
        if (grunnlag.getErBehovForPleie()) {
            return ja();
        }
        return nei(MedisinkevilkårAvslagsårsaker.IKKE_BEHOV_FOR_PLEIE.toRuleReason());
    }
}
