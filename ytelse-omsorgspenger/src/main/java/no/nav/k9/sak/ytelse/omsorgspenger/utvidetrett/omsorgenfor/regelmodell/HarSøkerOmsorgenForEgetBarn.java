package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell;

import java.util.Collection;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(HarSøkerOmsorgenForEgetBarn.ID)
public class HarSøkerOmsorgenForEgetBarn extends LeafSpecification<OmsorgenForVilkårGrunnlag> {

    static final String ID = "OMP_VK_9.5.1";

    HarSøkerOmsorgenForEgetBarn() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OmsorgenForVilkårGrunnlag grunnlag) {
        final var relasjon = grunnlag.getRelasjonMellomSøkerOgBarn();

        //TODO logikk bør splittes til å håndteres i regelmotoren?
        if (erMorEllerFarTilBarnet(relasjon) && (harSammeBosted(grunnlag) || harDeltBosted(grunnlag))) {
            return ja();
        }

        return nei(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason());
    }

    private boolean harDeltBosted(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getPerioderMedDeltBosted().values().stream()
            .flatMap(Collection::stream)
            .anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean harSammeBosted(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getBarnsAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean erMorEllerFarTilBarnet(Relasjon relasjon) {
        return relasjon != null && RelasjonsRolle.BARN == relasjon.getRelasjonsRolle();
    }
}
