package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(HarSøkerOmsorgenForPleietrengende.ID)
public class HarSøkerOmsorgenForPleietrengende extends LeafSpecification<OmsorgenForVilkårGrunnlag> {

    static final String ID = "PSB_VK_9.10.1";

    HarSøkerOmsorgenForPleietrengende() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OmsorgenForVilkårGrunnlag grunnlag) {
        if (grunnlag.getErOmsorgsPerson() != null && grunnlag.getErOmsorgsPerson()) {
            return ja();
        }
        
        final var relasjon = grunnlag.getRelasjonMellomSøkerOgPleietrengende();
        if (relasjon != null && erMorEllerFarTilPleietrengende(relasjon) && grunnlag.getErOmsorgsPerson() == null) {
            return ja();
        }

        return nei(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason());
    }

    @SuppressWarnings("unused")
    private boolean harSammeBosted(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getPleietrengendeAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean erMorEllerFarTilPleietrengende(Relasjon relasjon) {
        return !RelasjonsRolle.UKJENT.equals(relasjon.getRelasjonsRolle());
    }
}
