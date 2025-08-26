package no.nav.ung.sak.formidling.vedtak;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;

public interface VedtaksbrevGenerererTjeneste {

    //Bestilling
    @WithSpan
    GenerertBrev genererAutomatiskVedtaksbrev(Behandling behandling, DokumentMalType dokumentMalType, boolean kunHtml);

    // Bestilling
    @WithSpan
    GenerertBrev genererManuellVedtaksbrev(Long behandlingId, DokumentMalType originalDokumentMalType, String brevHtml, boolean kunHtml);
}
