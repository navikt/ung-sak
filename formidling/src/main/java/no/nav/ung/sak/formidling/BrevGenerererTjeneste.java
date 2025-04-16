package no.nav.ung.sak.formidling;

import io.opentelemetry.instrumentation.annotations.WithSpan;

public interface BrevGenerererTjeneste {
    @WithSpan
    GenerertBrev genererVedtaksbrev(Long behandlingId, boolean kunHtml);

    GenerertBrev genererBrevOverstyrRegler(Long behandlingId, boolean kunHtml);
}
