package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForAvslagsårsaker;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;

@RuleDocumentation(HarSøkerOmsorgenForBarn.ID)
public class HarSøkerOmsorgenForBarn extends LeafSpecification<OmsorgenForVilkårGrunnlag> {

    static final String ID = "OMS_VK_9.5.1_2";

    HarSøkerOmsorgenForBarn() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OmsorgenForVilkårGrunnlag grunnlag) {

        if (grunnlag.getHarBlittVurdertSomOmsorgsPerson() != null && grunnlag.getHarBlittVurdertSomOmsorgsPerson()) {
            return ja();
        }

        if (harSammeBosted(grunnlag)) {
            return ja();
        }

        if (harDeltBosted(grunnlag)) {
            return ja();
        }

        if (!grunnlag.getFosterbarn().isEmpty()) {
            return ja();
        }

        return nei(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason());
    }

    private boolean harSammeBosted(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getPleietrengendeAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean harDeltBosted(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getDeltBostedsAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));

    }
}
