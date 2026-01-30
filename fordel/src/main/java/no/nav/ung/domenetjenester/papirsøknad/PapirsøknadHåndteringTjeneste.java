package no.nav.ung.domenetjenester.papirsøknad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.Kanal;
import no.nav.k9.felles.integrasjon.saf.Tema;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO;
import no.nav.ung.domenetjenester.arkiv.ArkivTjeneste;
import no.nav.ung.domenetjenester.arkiv.JournalpostInfo;
import no.nav.ung.domenetjenester.arkiv.journal.TilJournalføringTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.ArkivFilType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.VariantFormat;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.felles.typer.*;
import no.nav.ung.sak.formidling.bestilling.JournalpostType;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientImpl;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.mottak.dokumentmottak.UngdomsytelseSøknadMottaker;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramRegisterKlient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PapirsøknadHåndteringTjeneste {

    private PdfGenKlient pdfGenKlient;
    private DokArkivKlientImpl dokArkivKlientImpl;
    private TpsTjeneste tpsTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private ArkivTjeneste arkivTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private TilJournalføringTjeneste journalføringTjeneste;
    private UngdomsytelseSøknadMottaker ungdomsytelseSøknadMottaker;
    private UngOppgaveKlient ungOppgaveKlient;

    public PapirsøknadHåndteringTjeneste() {
        // For CDI
    }

    @Inject
    public PapirsøknadHåndteringTjeneste(
        PdfGenKlient pdfGenKlient,
        DokArkivKlientImpl dokArkivKlientImpl,
        TpsTjeneste tpsTjeneste,
        FagsakTjeneste fagsakTjeneste,
        UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient,
        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
        ArkivTjeneste arkivTjeneste,
        DokumentArkivTjeneste dokumentArkivTjeneste,
        PersoninfoAdapter personinfoAdapter,
        TilJournalføringTjeneste journalføringTjeneste,
        @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE) UngdomsytelseSøknadMottaker ungdomsytelseSøknadMottaker,
        UngOppgaveKlient ungOppgaveKlient
    ) {
        this.pdfGenKlient = pdfGenKlient;
        this.dokArkivKlientImpl = dokArkivKlientImpl;
        this.tpsTjeneste = tpsTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.ungdomsprogramRegisterKlient = ungdomsprogramRegisterKlient;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.arkivTjeneste = arkivTjeneste;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.journalføringTjeneste = journalføringTjeneste;
        this.ungdomsytelseSøknadMottaker = ungdomsytelseSøknadMottaker;
        this.ungOppgaveKlient = ungOppgaveKlient;
    }

    public Saksnummer journalførPapirsøknadMotFagsak(String deltakerIdent, JournalpostId journalpostId) {

        AktørId aktørId = personinfoAdapter.hentAktørIdForPersonIdent(PersonIdent.fra(deltakerIdent))
            .orElseThrow(() -> new IllegalArgumentException("Finner ikke aktørId for deltakerIdent"));

        UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO deltakelse = validerDeltakelseEksisterer(aktørId);
        Periode periode = new Periode(deltakelse.fraOgMed(), null);

        Fagsak fagsak = ungdomsytelseSøknadMottaker.finnEllerOpprettFagsakForIkkeDigitalBruker(FagsakYtelseType.UNGDOMSYTELSE, aktørId, periode.getFom(), periode.getTom());

        if (journalpostId != null && journalføringTjeneste.erAlleredeJournalført(journalpostId)) {
            throw new IllegalStateException("Journalpost er allerede journalført");
        }

        boolean ferdigJournalført = journalføringTjeneste.tilJournalføring(journalpostId, Optional.of(fagsak.getSaksnummer().getVerdi()), OmrådeTema.UNG, aktørId.getAktørId());
        if (!ferdigJournalført) {
            throw new IllegalStateException("Journalpost kunne ikke journalføres");
        }

        return fagsak.getSaksnummer();
    }

    public OpprettJournalpostResponse opprettJournalpostForInnsendtPapirsøknad(PersonIdent deltakerIdent, JournalpostId journalpostId) {
        Personinfo personinfo = tpsTjeneste.hentBrukerForFnr(deltakerIdent).orElseThrow();
        String deltakerNavn = personinfo.getNavn();
        AktørId aktørId = personinfo.getAktørId();

        UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO deltakelse = validerDeltakelseEksisterer(aktørId);
        UUID deltakelseId = deltakelse.id();
        LocalDate startdato = deltakelse.fraOgMed();

        Fagsak fagsak = validerFagsakEksisterer(aktørId);

        OrganisasjonsEnhet behandlendeEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);

        byte[] pdfDokument = lagPdfDokument(deltakerIdent, startdato, deltakerNavn);
        byte[] jsonDokument = lagJsonDokument(deltakerIdent, startdato, deltakelseId, journalpostId);

        //Dette kallet er idempotenet. Hvis oppgaven er løst tidligere så vil ikke det feile ved et nytt kall her.
        ungOppgaveKlient.løsSøkYtelseOppgave(new DeltakerDTO(null, deltakerIdent.getIdent()));
        return opprettJournalpost(deltakerIdent, deltakerNavn, deltakelseId, pdfDokument, jsonDokument, behandlendeEnhet);
    }

    public PapirsøknadPdf hentDokumentForJournalpostId(JournalpostId journalpostId) {
        String dokumentInfoId = hentDokumentInfoId(journalpostId);
        byte[] dokument = dokumentArkivTjeneste.hentDokument(journalpostId, dokumentInfoId);
        return new PapirsøknadPdf(dokument, lagFilnavnForDokumentId(dokumentInfoId));
    }

    private String lagFilnavnForDokumentId(String dokumentId) {
        return "søknadsdokument-" + dokumentId + ".pdf";
    }

    private String hentDokumentInfoId(JournalpostId journalpostId) {
        JournalpostInfo journalpostInfo = arkivTjeneste.hentJournalpostInfo(journalpostId);
        String dokumentInfoId = journalpostInfo.getDokumentInfoId();
        if (dokumentInfoId == null) {
            throw new IllegalArgumentException("Finner ikke filnavn for journalpost " + journalpostId.getVerdi());
        }
        return dokumentInfoId;
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
