package no.nav.ung.sak.formidling.domene;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.dto.PartResponseDto;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;

public record GenerertBrev(

    /*
     * pdf og html dokument
     */
    PdfGenDokument dokument,

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
