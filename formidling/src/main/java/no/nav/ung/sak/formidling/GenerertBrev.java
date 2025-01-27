package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.template.TemplateType;

public record GenerertBrev(

    /*
     * pdf og html dokument
     */
    PdfGenDokument dokument,

    /*
     * Mottaker av brevet
     */
    PdlPerson mottaker,

    /*
     * Hvem brevet gjelder. Kan være en annen enn mottaker
     */
    PdlPerson gjelder,

    /*
     * MalType brukt
     */
    DokumentMalType malType,

    /*
     * TemplateType brukt
     */
    TemplateType templateType


) {
}
