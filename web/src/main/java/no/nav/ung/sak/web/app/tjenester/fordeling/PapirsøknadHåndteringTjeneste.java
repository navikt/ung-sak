package no.nav.ung.sak.web.app.tjenester.fordeling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.Kanal;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.ung.domenetjenester.arkiv.dok.model.Sakstype;
import no.nav.ung.kodeverk.dokument.ArkivFilType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.VariantFormat;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientImpl;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;
import org.jose4j.json.JsonUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PapirsøknadHåndteringTjeneste {

    private PdfGenKlient pdfGenKlient;
    private DokArkivKlientImpl dokArkivKlientImpl;
    private TpsTjeneste tpsTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private AktørTjeneste aktørTjeneste;

    public PapirsøknadHåndteringTjeneste() {
        // For CDI
    }

    @Inject
    public PapirsøknadHåndteringTjeneste(PdfGenKlient pdfGenKlient, DokArkivKlientImpl dokArkivKlientImpl, TpsTjeneste tpsTjeneste, FagsakTjeneste fagsakTjeneste, AktørTjeneste aktørTjeneste) {
        this.pdfGenKlient = pdfGenKlient;
        this.dokArkivKlientImpl = dokArkivKlientImpl;
        this.tpsTjeneste = tpsTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.aktørTjeneste = aktørTjeneste;
    }

    public PdfGenDokument journalførPapirsøknad(PersonIdent deltakerIdent, LocalDate startdato, UUID deltakelseId, JournalpostId journalpostId) {
        Personinfo personinfo = tpsTjeneste.hentBrukerForFnr(deltakerIdent).orElseThrow();
        String deltakerNavn = personinfo.getNavn();
        PdfGenDokument pdfDokument = lagPdfDokumentForPapirsøknad(new PapirsøknadDto(
            deltakerNavn,
            deltakerIdent.getIdent(),
            startdato,
            LocalDate.now()
        ));
        Søknad jsonSøknad = PapirsøknadtilK9FormatSøknadMapper.mapTilSøknad(new Papirsøknadopplysninger(NorskIdentitetsnummer.of(deltakerIdent.getIdent()), startdato, deltakelseId, journalpostId));
        byte[] jsonDokument = JsonUtils.toString(jsonSøknad).getBytes();
        // TODO: Journalfør i dokarkiv
        var aktørId = aktørTjeneste.hentAktørIdForPersonIdent(deltakerIdent).orElseThrow();
        var saksnummer = fagsakTjeneste.finnFagsakerForAktør(aktørId).getFirst().getSaksnummer().getVerdi();
        dokArkivKlientImpl.opprettJournalpost(new OpprettJournalpostRequestBuilder()
            .bruker(new OpprettJournalpostRequest.Bruker(deltakerIdent.getIdent(), OpprettJournalpostRequest.Bruker.BrukerIdType.FNR))
            .tema("UNG")
            .sak(new OpprettJournalpostRequest.Sak(Sakstype.FAGSAK.name(), saksnummer, "UNG_SAK"))
            .tittel("Punsjet papirsøknad om ungdomsprogramytelsen")
            .kanal(Kanal.NAV_NO.name())
            .journalfoerendeEnhet("9999")
            .eksternReferanseId(deltakelseId.toString())
            .dokumenter(List.of(
                new OpprettJournalpostRequest.Dokument(
                    "Punsjet papirsøknad om ungdomsprogramytelsen",
                    Brevkode.UNGDOMSYTELSE_SOKNAD_KODE,
                    "SOK",
                    List.of(
                        new OpprettJournalpostRequest.DokumentVariantArkivertPDFA(pdfDokument.pdf()),
                        new OpprettJournalpostRequest.DokumentVariantArkivertPDFA(ArkivFilType.JSON.getKode(), VariantFormat.ORIGINAL.getOffisiellKode(), jsonDokument)
                    )
                )
            ))
            .build()
        );

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
