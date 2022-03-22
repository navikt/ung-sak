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
        var relasjon = grunnlag.getRelasjonMellomSøkerOgBarn();

        boolean erForelderTilBarnet = erForelderTilBarnet(relasjon);
        boolean harSammeBosted = harSammeAdresse(grunnlag) || harDeltBosted(grunnlag);
        if (erForelderTilBarnet && harSammeBosted) {
            return ja();
        }

        return nei(OmsorgenForKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR.toRuleReason());
    }

    private boolean harDeltBosted(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getPerioderMedDeltBosted().values().stream()
            .flatMap(Collection::stream)
            .anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean harSammeAdresse(OmsorgenForVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getBarnsAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private boolean erForelderTilBarnet(Relasjon relasjon) {
        return relasjon != null && RelasjonsRolle.BARN == relasjon.getRelasjonsRolle();
    }
}
