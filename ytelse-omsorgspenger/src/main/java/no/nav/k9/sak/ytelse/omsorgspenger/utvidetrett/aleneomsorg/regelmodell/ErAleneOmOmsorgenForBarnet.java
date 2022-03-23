package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import java.util.List;

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
        if (!søkerHarSammeAdresseSomEnAnnenForelder(grunnlag)) {
            return ja();
        }

        return nei(AleneOmOmsorgenKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR.toRuleReason());
    }

    private boolean søkerHarSammeAdresseSomEnAnnenForelder(AleneOmOmsorgenVilkårGrunnlag grunnlag) {
        List<BostedsAdresse> andreForeldresAdresser = finnAdresserForForeldreUtenomSøker(grunnlag);
        List<BostedsAdresse> søkersAdresser = grunnlag.getSøkerAdresser();
        return andreForeldresAdresser.stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresse));
    }

    private List<BostedsAdresse> finnAdresserForForeldreUtenomSøker(AleneOmOmsorgenVilkårGrunnlag grunnlag) {
        return grunnlag.getForeldreAdresser().entrySet().stream()
            .filter(e -> !e.getKey().equals(grunnlag.getSøkerAktørId()))
            .flatMap(e -> e.getValue().stream())
            .toList();
    }

}
