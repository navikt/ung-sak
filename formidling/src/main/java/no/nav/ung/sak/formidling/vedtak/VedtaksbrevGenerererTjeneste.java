package no.nav.ung.sak.formidling.vedtak;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.GenerertBrev;

public interface VedtaksbrevGenerererTjeneste {

    @WithSpan
    GenerertBrev genererAutomatiskVedtaksbrev(VedtaksbrevGenerererInput vedtaksbrevGenereringInput);

    @WithSpan
    GenerertBrev genererManuellVedtaksbrev(Long behandlingId, DokumentMalType originalDokumentMalType, boolean kunHtml);
}
