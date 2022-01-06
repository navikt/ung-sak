package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;


@RuleDocumentation(value = PleiesHjemmeVilkår.ID, specificationReference = "")
public class PleiesHjemmeVilkår implements RuleService<PleiesHjemmeVilkårGrunnlag> {

    public static final String ID = "PLS_VK 9.13";

    @Override
    public Evaluation evaluer(PleiesHjemmeVilkårGrunnlag grunnlag, Object resultatStruktur) {
        final var mellomregningData = new PleiesHjemmeMellomregningData(grunnlag);

        final var evaluate = getSpecification().evaluate(mellomregningData);

        mellomregningData.oppdaterResultat((PleiesHjemmeVilkårResultat) resultatStruktur);

        return evaluate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<PleiesHjemmeMellomregningData> getSpecification() {
        Ruleset<PleiesHjemmeMellomregningData> rs = new Ruleset<>();
        return rs.hvisRegel(BeregnPleielokasjon.ID, "Er pleietrengende hjemme?")
            .hvis(new BeregnPleielokasjon(), new ErPleietrengendeIHjemmet())
            .ellers(new IkkeOppfylt(PleiesHjemmeVilkårAvslagsårsaker.PLEIETRENGENDE_INNLAGT_I_STEDET_FOR_HJEMME.toRuleReason()));
    }
}
