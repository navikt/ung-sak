package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.template.dto.TomVedtaksbrevMalDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

@Dependent
public class TomVedtaksbrevInnholdBygger implements VedtaksbrevInnholdBygger {

    @Inject
    public TomVedtaksbrevInnholdBygger() {
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        return new TemplateInnholdResultat(TemplateType.TOM_VEDTAKSBREV_MAL, new TomVedtaksbrevMalDto(), false);
    }
}
