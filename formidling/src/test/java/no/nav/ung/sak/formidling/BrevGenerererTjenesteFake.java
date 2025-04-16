package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.typer.AktørId;

public class BrevGenerererTjenesteFake implements BrevGenerererTjeneste {

    private final GenerertBrev resultat;

    public BrevGenerererTjenesteFake(String fnr) {
        this.resultat = new GenerertBrev(
            new PdfGenDokument(new byte[0], ""),
            new PdlPerson(fnr, new AktørId(123L), "Nordmann"),
            new PdlPerson(fnr, new AktørId(123L), "Nordmann"),
            DokumentMalType.INNVILGELSE_DOK,
            TemplateType.INNVILGELSE
        );
    }


    @Override
    public GenerertBrev genererVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return resultat;
    }

    public GenerertBrev genererVedtaksbrevKunHtml(Long behandlingId) {
        return resultat;
    }

    @Override
    public GenerertBrev genererBrevOverstyrRegler(Long behandlingId, boolean kunHtml) {
        return resultat;
    }
}
