package no.nav.ung.sak.web.app.tjenester.fordeling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.Kanal;
import no.nav.k9.felles.integrasjon.saf.Tema;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.ArkivFilType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.VariantFormat;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.formidling.bestilling.JournalpostType;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientImpl;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramRegisterKlient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PapirsøknadHåndteringTjeneste {

    private PdfGenKlient pdfGenKlient;
    private DokArkivKlientImpl dokArkivKlientImpl;
    private TpsTjeneste tpsTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    public PapirsøknadHåndteringTjeneste() {
        // For CDI
    }

    @Inject
    public PapirsøknadHåndteringTjeneste(PdfGenKlient pdfGenKlient, DokArkivKlientImpl dokArkivKlientImpl, TpsTjeneste tpsTjeneste, FagsakTjeneste fagsakTjeneste, UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.pdfGenKlient = pdfGenKlient;
        this.dokArkivKlientImpl = dokArkivKlientImpl;
        this.tpsTjeneste = tpsTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.ungdomsprogramRegisterKlient = ungdomsprogramRegisterKlient;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    public OpprettJournalpostResponse journalførPapirsøknad(PersonIdent deltakerIdent, LocalDate startdato, JournalpostId journalpostId) {
        Personinfo personinfo = tpsTjeneste.hentBrukerForFnr(deltakerIdent).orElseThrow();
        String deltakerNavn = personinfo.getNavn();
        AktørId aktørId = personinfo.getAktørId();

        UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO deltakelse = validerDeltakelseEksisterer(aktørId);
        UUID deltakelseId = deltakelse.id();

        Fagsak fagsak = validerFagsakEksisterer(aktørId);

        OrganisasjonsEnhet behandlendeEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);

        byte[] pdfDokument = lagPdfDokument(deltakerIdent, startdato, deltakerNavn);
        byte[] jsonDokument = lagJsonDokument(deltakerIdent, startdato, deltakelseId, journalpostId);

        return opprettJournalpost(deltakerIdent, deltakerNavn, deltakelseId, pdfDokument, jsonDokument, behandlendeEnhet);
    }

    private Fagsak validerFagsakEksisterer(AktørId aktørId) {
        return fagsakTjeneste.finnesEnFagsakSomOverlapper(FagsakYtelseType.UNGDOMSYTELSE, aktørId, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .orElseThrow(() -> new IllegalStateException("Finner ikke fagsak for deltaker " + " ved journalføring av papirsøknad."));
    }

    private UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO validerDeltakelseEksisterer(AktørId aktørId) {
        List<UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO> deltakelser = ungdomsprogramRegisterKlient.hentForAktørId(aktørId.getAktørId()).opplysninger();
        boolean deltakelseIkkeEksister = deltakelser.isEmpty();

        if (deltakelseIkkeEksister) {
            throw new IllegalStateException("Finner ikke deltakelse for deltaker ved journalføring av papirsøknad.");
        }

        if (deltakelser.size() > 1) {
            throw new IllegalStateException("Forventet kun en deltakelse for deltaker, fant " + deltakelser.size() + " ved journalføring av papirsøknad.");
        }

        return deltakelser.getFirst();
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


    private OpprettJournalpostResponse opprettJournalpost(PersonIdent deltakerIdent, String deltakerNavn, UUID deltakelseId, byte[] pdfDokument, byte[] jsonDokument, OrganisasjonsEnhet behandlendeEnhet) {
        String ungdomsytelseSoknadOffisiellKode = Brevkode.UNGDOMSYTELSE_SOKNAD.getOffisiellKode();
        String journalpostTittel = "Punsjet søknad om ungdomsprogramytelse - " + ungdomsytelseSoknadOffisiellKode;

        return dokArkivKlientImpl.opprettJournalpost(new OpprettJournalpostRequestBuilder()
            .bruker(new OpprettJournalpostRequest.Bruker(deltakerIdent.getIdent(), OpprettJournalpostRequest.Bruker.BrukerIdType.FNR))
            .tema(Tema.UNG.name())
            .tittel(journalpostTittel)
            .kanal(Kanal.NAV_NO.name())
            .journalfoerendeEnhet(behandlendeEnhet.getEnhetId())
            .eksternReferanseId(deltakelseId.toString())
            .behandlingstema(BehandlingTema.UNGDOMSPROGRAMYTELSEN)
            .avsenderMottaker(new OpprettJournalpostRequest.AvsenderMottaker(deltakerIdent.getIdent(), deltakerNavn, null, OpprettJournalpostRequest.AvsenderMottaker.IdType.FNR))
            .journalpostType(JournalpostType.INNGAAENDE)
            .dokumenter(List.of(
                new OpprettJournalpostRequest.Dokument(
                    journalpostTittel,
                    ungdomsytelseSoknadOffisiellKode,
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
