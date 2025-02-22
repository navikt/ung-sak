package no.nav.ung.sak.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

public interface VedtaksbrevInnholdBygger {

    TemplateInnholdResultat bygg(Behandling behandlingId, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje);

}


