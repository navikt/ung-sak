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
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslagInngangsvilkårDto;
import org.slf4j.Logger;

@Dependent
public class FørstegangsAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FørstegangsAvslagInnholdBygger.class);


    @Inject
    public FørstegangsAvslagInnholdBygger() {
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {

        LocalDateTimeline<DetaljertResultat> periode = DetaljertResultat.filtererTidslinje(detaljertResultatTidslinje, DetaljertResultatType.AVSLAG_INNGANGSVILKÅR);

        var fom = periode.getMinLocalDate();

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG,
            new AvslagInngangsvilkårDto(fom));
    }

}
