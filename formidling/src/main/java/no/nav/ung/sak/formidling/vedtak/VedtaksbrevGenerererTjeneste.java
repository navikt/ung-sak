package no.nav.ung.sak.formidling.vedtak;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import no.nav.ung.sak.formidling.GenerertBrev;

public interface VedtaksbrevGenerererTjeneste {
    @WithSpan
    GenerertBrev genererVedtaksbrevForBehandling(VedtaksbrevBestillingInput vedtaksbrevBestillingInput);
    @WithSpan
    GenerertBrev genererManuellVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput);
    @WithSpan
    GenerertBrev genererAutomatiskVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput);
}
