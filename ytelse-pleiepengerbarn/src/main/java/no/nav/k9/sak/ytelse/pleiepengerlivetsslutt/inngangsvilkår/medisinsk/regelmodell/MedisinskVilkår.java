package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.inngangsvilkår.IkkeOppfylt;

@RuleDocumentation(value = MedisinskVilkår.ID, specificationReference = "")
public class MedisinskVilkår implements RuleService<MedisinskVilkårGrunnlag> {

    public static final String ID = "PLS_VK 9.13";

    @Override
    public Evaluation evaluer(MedisinskVilkårGrunnlag grunnlag, Object resultatStruktur) {
        final var mellomregningData = new MedisinskMellomregningData(grunnlag);

        final var evaluate = getSpecification().evaluate(mellomregningData);

        mellomregningData.oppdaterResultat((MedisinskVilkårResultat) resultatStruktur);

        return evaluate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<MedisinskMellomregningData> getSpecification() {
        Ruleset<MedisinskMellomregningData> rs = new Ruleset<>();
        return rs.hvisRegel(ErPleietrengendeILivetsSluttfase.ID, "Er pleietrengende i livets sluttfase?")
            .hvis(new ErPleietrengendeILivetsSluttfase(), rs.hvisRegel(BeregnPleielokasjon.ID,  "Er pleietrengende hjemme?")
                .hvis(new BeregnPleielokasjon(), new ErPleietrengendeIHjemmet())
                .ellers(new IkkeOppfylt(MedisinskVilkårAvslagsårsaker.PLEIETRENGENDE_INNLAGT_I_STEDET_FOR_HJEMME.toRuleReason())))
            .ellers(new IkkeOppfylt(MedisinskVilkårAvslagsårsaker.IKKE_I_LIVETS_SLUTTFASE.toRuleReason()));

    }
}
