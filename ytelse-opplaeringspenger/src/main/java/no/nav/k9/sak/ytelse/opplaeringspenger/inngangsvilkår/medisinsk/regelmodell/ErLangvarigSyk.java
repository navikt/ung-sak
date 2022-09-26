package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import java.util.Objects;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ErLangvarigSyk.ID)
public class ErLangvarigSyk extends LeafSpecification<MedisinskMellomregningData> {

    public static final String ID = "OLP_VK_9.16.5";

    public ErLangvarigSyk() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {
        var grunnlag = mellomregning.getGrunnlag();

        mellomregning.registrerDokumentasjonLangvarigSykdom(grunnlag.getDokumentertLangvarigSykdomPerioder());

        var godkjentTidslinje = mellomregning.getVurdertTidslinje().filterValue(it -> Objects.equals(it, LangvarigSykdomDokumentasjon.DOKUMENTERT));

        var evaluation = !godkjentTidslinje.isEmpty() ? ja() : nei();

        evaluation.setEvaluationProperty(MedisinskVilkårResultat.DOKUMENTASJON_LANGVARIG_SYKDOM_PERIODER, godkjentTidslinje);

        return evaluation;
    }
}
