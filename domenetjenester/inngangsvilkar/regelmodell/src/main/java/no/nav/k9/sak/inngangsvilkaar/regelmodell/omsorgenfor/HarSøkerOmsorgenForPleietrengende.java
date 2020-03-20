package no.nav.k9.sak.inngangsvilkaar.regelmodell.omsorgenfor;

import java.util.Objects;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(HarSøkerOmsorgenForPleietrengende.ID)
public class HarSøkerOmsorgenForPleietrengende extends LeafSpecification<OmsorgenForGrunnlag> {

    static final String ID = "PSB_VK_9.10.1";

    HarSøkerOmsorgenForPleietrengende() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OmsorgenForGrunnlag grunnlag) {

        final var relasjon = grunnlag.getRelasjonMellomSøkerOgPleietrengende();
        if (relasjon != null) {
            if (erMorEllerFarTilPleietrengende(relasjon) && relasjon.getHarSammeBosted()) {
                return ja();
            } else if (erMorEllerFarTilPleietrengende(relasjon) && saksbehandlerBekreftetOmsorgen(grunnlag)) {
                return ja();
            }
        }

        if (harSammeBosted(grunnlag) && saksbehandlerBekreftetOmsorgen(grunnlag)) {
            return ja();
        }

        return nei(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason());
    }

    private boolean saksbehandlerBekreftetOmsorgen(OmsorgenForGrunnlag grunnlag) {
        return Objects.equals(grunnlag.getErOmsorgsPerson(), true);
    }

    private boolean harSammeBosted(OmsorgenForGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getPleietrengendeAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean erMorEllerFarTilPleietrengende(Relasjon relasjon) {
        return !RelasjonsRolle.UKJENT.equals(relasjon.getRelasjonsRolle());
    }
}
