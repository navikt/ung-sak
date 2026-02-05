package no.nav.ung.sak.formidling.vedtak;

import no.nav.ung.kodeverk.dokument.DokumentMalType;

public record VedtaksbrevForhåndsvisInput(
    Long behandlingId,
    DokumentMalType dokumentMalType,
    Boolean redigertVersjon,
    boolean htmlVersjon
) {
}
