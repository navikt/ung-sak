package no.nav.ung.sak.formidling.vedtak;

import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;

public record VedtaksbrevGenerererInput(Long behandlingId, Vedtaksbrev vedtaksbrev,
                                        DetaljertResultatTidslinje detaljertResultatTidslinje, boolean kunHtml
) {
}
