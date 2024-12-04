package no.nav.ung.sak.formidling.dto;

import no.nav.ung.kodeverk.dokument.DokumentMalType;

public record GenerertBrev(
    /*
     * PDF dokumentet
     */
    byte[] pdfData,
    /*
     * Mottaker av brevet
     */
    PartResponseDto mottaker,

    /*
     * Hvem brevet gjelder. Kan være en annen enn mottaker
     */
    PartResponseDto gjelder,

    /*
     * MalType brukt
     */
    DokumentMalType malType

) {
}
