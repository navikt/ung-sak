package no.nav.k9.sak.inngangsvilkaar.regelmodell.medisinsk;

import java.util.List;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(HarBarnetEnSykdomSkadeEllerLyteDokumenterFraRettOrgan.ID)
public class HarBarnetEnSykdomSkadeEllerLyteDokumenterFraRettOrgan extends LeafSpecification<MedisinskMellomregningData> {

    static final String ID = "PSB_VK_9.10.1";

    HarBarnetEnSykdomSkadeEllerLyteDokumenterFraRettOrgan() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {
        final var grunnlag = mellomregning.getGrunnlag();

        if (harIkkeSykdomSkadeEllerLyte(grunnlag.getDiagnoseKode())) {
            return nei(MedisinskeVilkårAvslagsårsaker.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE.toRuleReason());
        }
        if (harIkkeDokumentasjonFraRettOrgan(grunnlag)) { // TODO: første 8 uker == må være sykehuslege
            return nei(MedisinskeVilkårAvslagsårsaker.DOKUMENTASJON_IKKE_FRA_RETT_ORGAN.toRuleReason());
        }
        return ja();
    }

    private boolean harIkkeDokumentasjonFraRettOrgan(MedisinskvilkårGrunnlag grunnlag) {
        return grunnlag.getDiagnoseKilde() == null ||
            !List.of(DiagnoseKilde.SYKHUSLEGE, DiagnoseKilde.SPESIALISTHELSETJENESTEN).contains(grunnlag.getDiagnoseKilde());
    }

    private boolean harIkkeSykdomSkadeEllerLyte(String diagnoseKode) {
        return !(diagnoseKode != null && !diagnoseKode.isBlank() && !diagnoseKode.isEmpty());
    }
}
