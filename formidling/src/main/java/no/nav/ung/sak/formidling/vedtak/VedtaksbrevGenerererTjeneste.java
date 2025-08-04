package no.nav.ung.sak.formidling.vedtak;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import no.nav.ung.sak.formidling.GenerertBrev;

public interface VedtaksbrevGenerererTjeneste {
    @WithSpan
    GenerertBrev genererVedtaksbrevForBehandling(Long behandlingId, boolean kunHtml);
    @WithSpan
    GenerertBrev genererManuellVedtaksbrev(Long behandlingId, boolean kunHtml);
    @WithSpan
    GenerertBrev genererAutomatiskVedtaksbrev(Long behandlingId, boolean kunHtml);
}
