package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerBeregningsgrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.InnvilgelseDto;
import org.slf4j.Logger;

@Dependent
public class FørstegangsInnvilgelseInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FørstegangsInnvilgelseInnholdBygger.class);

    private final AktivitetspengerBeregningsgrunnlagRepository beregningsgrunnlagRepository;

    @Inject
    public FørstegangsInnvilgelseInnholdBygger(AktivitetspengerBeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {

        LocalDateTimeline<DetaljertResultat> periode = DetaljertResultat.filtererTidslinje(detaljertResultatTidslinje, DetaljertResultatType.INNVILGELSE_KUN_VILKÅR);

        var ytelseFom = periode.getMinLocalDate();
        var ytelseTom = periode.getMaxLocalDate();

        LocalDateTimeline<Beregningsgrunnlag> beregningsgrunnlagTidslinje = beregningsgrunnlagRepository.hentBesteBeregningSomTidslinje(behandling.getId());
        var beregningsgrunnlag = beregningsgrunnlagTidslinje.toSegments().first().getValue();

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_INNVILGELSE,
            new InnvilgelseDto(
                ytelseFom,
                ytelseTom,
                beregningsgrunnlag.getSisteLignedeÅr(),
                beregningsgrunnlag.utledBesteBeregningResultatType(),
                beregningsgrunnlag.getBeregnetPrAar(),
                beregningsgrunnlag.getDagsats()
        ));
    }

}
