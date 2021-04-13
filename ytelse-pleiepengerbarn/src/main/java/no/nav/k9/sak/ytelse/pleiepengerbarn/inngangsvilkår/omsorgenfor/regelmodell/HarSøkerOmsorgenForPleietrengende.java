package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell;

import java.util.Objects;

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

        final var relasjon = grunnlag.getRelasjonMellomSøkerOgPleietrengende();
        if (relasjon != null && erMorEllerFarTilPleietrengende(relasjon) && grunnlag.getErOmsorgsPerson() == null) {
            return ja();
        }
        if (grunnlag.getErOmsorgsPerson() != null && grunnlag.getErOmsorgsPerson()) {
            return ja();
        }

        return nei(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason());
    }

    private boolean saksbehandlerBekreftetOmsorgen(OmsorgenForVilkårGrunnlag grunnlag) {
        return Objects.equals(grunnlag.getErOmsorgsPerson(), true);
    }

    private boolean harSammeBosted(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getPleietrengendeAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean erMorEllerFarTilPleietrengende(Relasjon relasjon) {
        return !RelasjonsRolle.UKJENT.equals(relasjon.getRelasjonsRolle());
    }
}
