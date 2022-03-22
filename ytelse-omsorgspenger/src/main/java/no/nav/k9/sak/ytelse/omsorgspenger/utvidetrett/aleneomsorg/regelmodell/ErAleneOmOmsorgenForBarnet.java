package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErAleneOmOmsorgenForBarnet.ID)
public class ErAleneOmOmsorgenForBarnet extends LeafSpecification<AleneOmOmsorgenVilkårGrunnlag> {

    static final String ID = "OMP_VK_9.6.1";

    ErAleneOmOmsorgenForBarnet() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(AleneOmOmsorgenVilkårGrunnlag grunnlag) {
        boolean harIngenAnnenForelder = grunnlag.getAnnenForelder().isEmpty();
        boolean foreldreHarIkkeSammeAdresse = !foreldreHarSammeAdresse(grunnlag);
        if (harIngenAnnenForelder || foreldreHarIkkeSammeAdresse) {
            return ja();
        }

        return nei(AleneOmOmsorgenKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR.toRuleReason());
    }

    private boolean foreldreHarSammeAdresse(AleneOmOmsorgenVilkårGrunnlag grunnlag) {
        final var søkersAdresser = grunnlag.getSøkersAdresser();
        return grunnlag.getAndreForeldersAdresser().stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

}
