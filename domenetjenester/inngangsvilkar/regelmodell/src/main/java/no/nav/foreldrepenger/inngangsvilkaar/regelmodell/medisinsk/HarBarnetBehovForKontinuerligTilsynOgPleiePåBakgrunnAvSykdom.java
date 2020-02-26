package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(HarBarnetBehovForKontinuerligTilsynOgPleiePåBakgrunnAvSykdom.ID)
public class HarBarnetBehovForKontinuerligTilsynOgPleiePåBakgrunnAvSykdom extends LeafSpecification<MedisinskMellomregningData> {

    static final String ID = "PSB_VK_9.10.3";

    HarBarnetBehovForKontinuerligTilsynOgPleiePåBakgrunnAvSykdom() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {

        final var ikkeBehovForKontinuerligTilsynOgPleie = mellomregning.getPerioderMedPleieOgGrad()
            .stream()
            .allMatch(p -> Pleiegrad.INGEN.equals(p.getGrad()));

        if (ikkeBehovForKontinuerligTilsynOgPleie) {
            return nei(MedisinskeVilkårAvslagsårsaker.IKKE_BEHOV_FOR_KONTINUERLIG_PLEIE.toRuleReason());
        }

        return ja();
    }
}
