package no.nav.ung.sak.web.app.tjenester.fordeling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientImpl;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;

@ApplicationScoped
public class PapirsøknadHåndteringTjeneste {

    private PdfGenKlient pdfGenKlient;
    private DokArkivKlientImpl dokArkivKlientImpl;
    private TpsTjeneste tpsTjeneste;

    public PapirsøknadHåndteringTjeneste() {
        // For CDI
    }

    @Inject
    public PapirsøknadHåndteringTjeneste(PdfGenKlient pdfGenKlient, DokArkivKlientImpl dokArkivKlientImpl, TpsTjeneste tpsTjeneste) {
        this.pdfGenKlient = pdfGenKlient;
        this.dokArkivKlientImpl = dokArkivKlientImpl;
        this.tpsTjeneste = tpsTjeneste;
    }

    public PdfGenDokument journalførPapirsøknad(PersonIdent deltakerIdent, LocalDate startdato) {
        /*Personinfo personinfo = tpsTjeneste.hentBrukerForFnr(deltakerIdent).orElseThrow();
        String deltakerNavn = personinfo.getNavn();*/
        // TOOD: Hent navn fra PDL/TPS
        PdfGenDokument pdfDokument = lagPdfDokumentForPapirsøknad(new PapirsøknadDto(
            "deltakerNavn",
            deltakerIdent.getIdent(),
            startdato,
            LocalDate.now()
        ));

        // TODO: Journalfør i dokarkiv
        /*dokArkivKlientImpl.opprettJournalpost(new OpprettJournalpostRequestBuilder()
            .build()
        );*/

        return pdfDokument;
    }

    public PdfGenDokument lagPdfDokumentForPapirsøknad(PapirsøknadDto papirsøknadDto) {
        return pdfGenKlient.lagDokument(
            "punsjet_papirsøknad",
            "soknad",
            papirsøknadDto,
            false);
    }
}
