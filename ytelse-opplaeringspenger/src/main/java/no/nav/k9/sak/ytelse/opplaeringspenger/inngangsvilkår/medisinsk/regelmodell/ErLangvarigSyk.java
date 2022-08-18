package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@RuleDocumentation(ErLangvarigSyk.ID)
public class ErLangvarigSyk extends LeafSpecification<MedisinskMellomregningData> {

    public static final String ID = "OLP_VK_9.X.X";

    public ErLangvarigSyk() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {
        var grunnlag = mellomregning.getGrunnlag();

        mellomregning.registrerDokumentasjonLangvarigSykdom(grunnlag.getDokumentertLangvarigSykdomPerioder());
        var dokumentasjonsperioder = mellomregning.getDokumentasjonStatusLangvarigSykdomPerioder();

        var vilkårsperiode = new LocalDateTimeline<>(grunnlag.getFom(), grunnlag.getTom(), (Void) null);

        var evaluation = vilkårsperiode.intersects(grunnlag.getDokumentertLangvarigSykdomPerioder()) ? ja() : nei();

        evaluation.setEvaluationProperty(MedisinskVilkårResultat.DOKUMENTASJON_LANGVARIG_SYKDOM_PERIODER, dokumentasjonsperioder);

        return evaluation;
    }
}
