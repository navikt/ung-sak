package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(BeregnPleielokasjon.ID)
public class BeregnPleielokasjon extends LeafSpecification<MedisinskMellomregningData> {

    static final String ID = "PLS_VK_9.13.1";

    BeregnPleielokasjon() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {
        final var evaluation = ja();

        final var grunnlag = mellomregning.getGrunnlag();

        grunnlag.getInnleggelsesPerioder().forEach(mellomregning::addInnleggelsePeriode);
        // Perioder hvor pleietrengende ikke er innlagt antas å pleies hjemme (søker har "Ja" i Søknadsdialog på dette)
        evaluation.setEvaluationProperty(MedisinskVilkårResultat.PLEIEPERIODER_MED_PLEIELOKASJON, mellomregning.getBeregnedePerioderMedPleielokasjon());

        return evaluation;
    }
}
