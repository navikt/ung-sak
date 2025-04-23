package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
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
    public GenerertBrev genererVedtaksbrevForBehandling(Long behandlingId, boolean kunHtml) {
        return resultat;
    }

    @Override
    public GenerertBrev genererManuellVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return resultat;
    }

    @Override
    public GenerertBrev genererAutomatiskVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return resultat;
    }
}
