package no.nav.ung.sak.formidling;

import io.opentelemetry.instrumentation.annotations.WithSpan;

public interface VedtaksbrevGenerererTjeneste {
    @WithSpan
    GenerertBrev genererVedtaksbrevForBehandling(Long behandlingId, boolean kunHtml);
    @WithSpan
    GenerertBrev genererManuellVedtaksbrev(Long behandlingId, boolean kunHtml);
    @WithSpan
    GenerertBrev genererAutomatiskVedtaksbrev(Long behandlingId, boolean kunHtml);
}
