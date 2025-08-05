package no.nav.ung.sak.formidling.vedtak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

public record VedtaksbrevBestillingInput(Long behandlingId, Vedtaksbrev vedtaksbrev,
                                         LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje, boolean kunHtml
) {
}
