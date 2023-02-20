package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@RuleDocumentation(ErPleietrengendeILivetsSluttfase.ID)
public class ErPleietrengendeILivetsSluttfase extends LeafSpecification<MedisinskMellomregningData> {

    public static final String ID = "PLS_VK_9.16.1";

    public ErPleietrengendeILivetsSluttfase() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedisinskMellomregningData mellomregning) {
        var grunnlag = mellomregning.getGrunnlag();

        mellomregning.registrerDokumentasjonLivetsSluttfase(grunnlag.getDokumentertLivetsSluttfasePerioder());
        var dokumentasjonsperioder = mellomregning.getDokumentasjonStatusLivetsSluttfasePerioder();

        var vilkårsperiode = new LocalDateTimeline<>(grunnlag.getFom(), grunnlag.getTom(), (Void) null);

        var evaluation = vilkårsperiode.intersects(grunnlag.getDokumentertLivetsSluttfasePerioder()) ? ja() : nei();

        evaluation.setEvaluationProperty(MedisinskVilkårResultat.DOKUMENTASJON_LIVETS_SLUTTFASE_PERIODER, dokumentasjonsperioder);

        return evaluation;
    }

}
