package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import java.util.List;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(HarAleneomsorgForBarnet.ID)
public class HarAleneomsorgForBarnet extends LeafSpecification<AleneomsorgVilkårGrunnlag> {

    static final String ID = "OMP_VK_9.6.1";

    HarAleneomsorgForBarnet() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(AleneomsorgVilkårGrunnlag grunnlag) {
        if (!søkerHarSammeAdresseSomEnAnnenForelder(grunnlag)) {
            return ja();
        }

        return kanIkkeVurdere(AleneomsorgKanIkkeVurdereAutomatiskÅrsaker.KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR.toRuleReason());
    }

    private boolean søkerHarSammeAdresseSomEnAnnenForelder(AleneomsorgVilkårGrunnlag grunnlag) {
        List<BostedsAdresse> andreForeldresAdresser = finnAdresserForForeldreUtenomSøker(grunnlag);
        List<BostedsAdresse> søkersAdresser = grunnlag.getSøkerAdresser();
        return andreForeldresAdresser.stream().anyMatch(it -> søkersAdresser.stream().anyMatch(it::erSammeAdresseOgOverlapperTidsmessig));
    }

    private List<BostedsAdresse> finnAdresserForForeldreUtenomSøker(AleneomsorgVilkårGrunnlag grunnlag) {
        return grunnlag.getForeldreAdresser().entrySet().stream()
            .filter(e -> !e.getKey().equals(grunnlag.getSøkerAktørId()))
            .flatMap(e -> e.getValue().stream())
            .toList();
    }

}
