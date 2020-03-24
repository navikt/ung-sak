package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(BeregnBehovForTilsynOgPleieOgAntallTilsynsPersoner.ID)
public class BeregnBehovForTilsynOgPleieOgAntallTilsynsPersoner extends LeafSpecification<MedisinskMellomregningData> {

    static final String ID = "PSB_VK_9.10.2";

    BeregnBehovForTilsynOgPleieOgAntallTilsynsPersoner() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {
        final var evaluation = ja();

        final var grunnlag = mellomregning.getGrunnlag();
        grunnlag.getInnleggelsesPerioder().forEach(mellomregning::addBehovsPeriode);
        grunnlag.getPerioderMedUtvidetBehov().forEach(mellomregning::addBehovsPeriode);
        grunnlag.getPerioderMedKontinuerligTilsyn().forEach(mellomregning::addBehovsPeriode);

        evaluation.setEvaluationProperty(MedisinskVilkårResultat.PERIODER_UTEN_TILSYN_OG_PLEIE, mellomregning.getPerioderUtenTilsynOgPleie());
        evaluation.setEvaluationProperty(MedisinskVilkårResultat.PERIODER_MED_EN_TILSYNSPERSONER, mellomregning.getPerioderMedEnTilsynsperson());
        evaluation.setEvaluationProperty(MedisinskVilkårResultat.PERIODER_MED_TO_TILSYNSPERSONER, mellomregning.getPerioderMedToTilsynsperson());

        return evaluation;
    }

}
