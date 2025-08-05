package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.formidling.mottaker.PdlPerson;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevBestillingInput;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;
import no.nav.ung.sak.typer.AktørId;

public class VedtaksbrevGenerererTjenesteFake implements VedtaksbrevGenerererTjeneste {

    private final GenerertBrev resultat;

    public VedtaksbrevGenerererTjenesteFake(String fnr) {
        this.resultat = new GenerertBrev(
            new PdfGenDokument(new byte[0], ""),
            new PdlPerson(fnr, new AktørId(123L), "Nordmann", null),
            new PdlPerson(fnr, new AktørId(123L), "Nordmann", null),
            DokumentMalType.INNVILGELSE_DOK,
            TemplateType.INNVILGELSE
        );
    }


    @Override
    public GenerertBrev genererAutomatiskVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput) {
        return resultat;
    }

    @Override
    public GenerertBrev genererManuellVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return resultat;
    }
}
