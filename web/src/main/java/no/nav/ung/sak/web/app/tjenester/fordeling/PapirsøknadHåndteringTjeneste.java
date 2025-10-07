package no.nav.ung.sak.web.app.tjenester.fordeling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.Journalposttype;
import no.nav.k9.felles.integrasjon.saf.Kanal;
import no.nav.k9.felles.integrasjon.saf.Tema;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.ung.kodeverk.dokument.ArkivFilType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.VariantFormat;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.formidling.bestilling.JournalpostType;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientImpl;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static no.nav.ung.domenetjenester.arkiv.dok.DokTjeneste.MASKINELL_JOURNALFØRENDE_ENHET;

@ApplicationScoped
public class PapirsøknadHåndteringTjeneste {

    private PdfGenKlient pdfGenKlient;
    private DokArkivKlientImpl dokArkivKlientImpl;
    private TpsTjeneste tpsTjeneste;

    public PapirsøknadHåndteringTjeneste() {
        // For CDI
    }

    @Inject
    public PapirsøknadHåndteringTjeneste(PdfGenKlient pdfGenKlient, DokArkivKlientImpl dokArkivKlientImpl, TpsTjeneste tpsTjeneste, FagsakTjeneste fagsakTjeneste, AktørTjeneste aktørTjeneste) {
        this.pdfGenKlient = pdfGenKlient;
        this.dokArkivKlientImpl = dokArkivKlientImpl;
        this.tpsTjeneste = tpsTjeneste;
    }

    public OpprettJournalpostResponse journalførPapirsøknad(PersonIdent deltakerIdent, LocalDate startdato, UUID deltakelseId, JournalpostId journalpostId) {
        Personinfo personinfo = tpsTjeneste.hentBrukerForFnr(deltakerIdent).orElseThrow();
        String deltakerNavn = personinfo.getNavn();

        byte[] pdfDokument = lagPdfDokument(deltakerIdent, startdato, deltakerNavn);
        byte[] jsonDokument = lagJsonDokument(deltakerIdent, startdato, deltakelseId, journalpostId);

        OpprettJournalpostResponse opprettJournalpostResponse = opprettJournalpost(deltakerIdent, deltakerNavn, deltakelseId, pdfDokument, jsonDokument);

        return opprettJournalpostResponse;
    }

    private static byte[] lagJsonDokument(PersonIdent deltakerIdent, LocalDate startdato, UUID deltakelseId, JournalpostId journalpostId) {
        Søknad jsonSøknad = PapirsøknadtilK9FormatSøknadMapper.mapTilSøknad(new Papirsøknadopplysninger(NorskIdentitetsnummer.of(deltakerIdent.getIdent()), startdato, deltakelseId, journalpostId));
        byte[] jsonDokument = JsonUtils.toString(jsonSøknad).getBytes();
        return jsonDokument;
    }

    private byte[] lagPdfDokument(PersonIdent deltakerIdent, LocalDate startdato, String deltakerNavn) {
        return pdfGenKlient.lagDokument(
            "punsjet_papirsøknad",
            "soknad",
            new PapirsøknadDto(
                deltakerNavn,
                deltakerIdent.getIdent(),
                startdato,
                LocalDate.now()
            ),
            false).pdf();
    }


    private OpprettJournalpostResponse opprettJournalpost(PersonIdent deltakerIdent, String deltakerNavn, UUID deltakelseId, byte[] pdfDokument, byte[] jsonDokument) {
        return dokArkivKlientImpl.opprettJournalpost(new OpprettJournalpostRequestBuilder()
            .bruker(new OpprettJournalpostRequest.Bruker(deltakerIdent.getIdent(), OpprettJournalpostRequest.Bruker.BrukerIdType.FNR))
            .tema(Tema.UNG.name())
            .tittel("Punsjet papirsøknad om ungdomsprogramytelsen")
            .kanal(Kanal.NAV_NO.name())
            .journalfoerendeEnhet(MASKINELL_JOURNALFØRENDE_ENHET)
            .eksternReferanseId(deltakelseId.toString())
            .behandlingstema("-")
            .avsenderMottaker(new OpprettJournalpostRequest.AvsenderMottaker(deltakerIdent.getIdent(), deltakerNavn, null, OpprettJournalpostRequest.AvsenderMottaker.IdType.FNR))
            .journalpostType(JournalpostType.INNGAAENDE)
            .dokumenter(List.of(
                new OpprettJournalpostRequest.Dokument(
                    "Punsjet papirsøknad om ungdomsprogramytelsen",
                    Brevkode.UNGDOMSYTELSE_SOKNAD.getOffisiellKode(),
                    "SOK",
                    List.of(
                        new OpprettJournalpostRequest.DokumentVariantArkivertPDFA(pdfDokument),
                        new OpprettJournalpostRequest.DokumentVariantArkivertPDFA(ArkivFilType.JSON.getKode(), VariantFormat.ORIGINAL.getOffisiellKode(), jsonDokument)
                    )
                )
            ))
            .build()
        );
    }
}
