package no.nav.ung.ytelse.ungdomsprogramytelsen.papirsøknad;

import no.nav.ung.brukerdialog.kontrakt.AktørIdDto;
import no.nav.ung.domenetjenester.arkiv.ArkivTjeneste;
import no.nav.ung.domenetjenester.arkiv.journal.TilJournalføringTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.mottak.SøknadMottakTjeneste;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PapirsøknadHåndteringTjenesteTest {

    private final PdfGenKlient pdfGenKlient = mock(PdfGenKlient.class);
    private final DokArkivKlient dokArkivKlient = mock(DokArkivKlient.class);
    private final TpsTjeneste tpsTjeneste = mock(TpsTjeneste.class);
    private final FagsakTjeneste fagsakTjeneste = mock(FagsakTjeneste.class);
    private final BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
    private final ArkivTjeneste arkivTjeneste = mock(ArkivTjeneste.class);
    private final DokumentArkivTjeneste dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
    private final PersoninfoAdapter personinfoAdapter = mock(PersoninfoAdapter.class);
    private final TilJournalføringTjeneste journalføringTjeneste = mock(TilJournalføringTjeneste.class);
    private final SøknadMottakTjeneste søknadMottakTjeneste = mock(SøknadMottakTjeneste.class);
    private final UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient = mock(UngdomsprogramRegisterKlient.class);
    private final UngBrukerdialogOppgaveKlient oppgaveKlient = mock(UngBrukerdialogOppgaveKlient.class);

    private PapirsøknadHåndteringTjeneste tjeneste;

    private static final String FNR = "14430175875";
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final UUID DELTAKELSE_ID = UUID.randomUUID();
    private static final LocalDate STARTDATO = LocalDate.of(2025, 1, 1);

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        jakarta.enterprise.inject.Instance<SøknadMottakTjeneste> instance = mock(jakarta.enterprise.inject.Instance.class);
        when(instance.get()).thenReturn(søknadMottakTjeneste);

        tjeneste = new PapirsøknadHåndteringTjeneste(
            pdfGenKlient,
            dokArkivKlient,
            tpsTjeneste,
            fagsakTjeneste,
            behandlendeEnhetTjeneste,
            arkivTjeneste,
            dokumentArkivTjeneste,
            personinfoAdapter,
            journalføringTjeneste,
            instance,
            ungdomsprogramRegisterKlient,
            oppgaveKlient
        );

        Personinfo personinfo = new Personinfo.Builder()
            .medAktørId(AKTØR_ID)
            .medPersonIdent(PersonIdent.fra(FNR))
            .medNavn("Test Testesen")
            .medFødselsdato(LocalDate.of(1990, 1, 1))
            .build();
        when(tpsTjeneste.hentBrukerForFnr(any(PersonIdent.class), eq(FagsakYtelseType.UNGDOMSYTELSE)))
            .thenReturn(Optional.of(personinfo));

        UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO deltakelse = new UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO(
            DELTAKELSE_ID, FNR, STARTDATO, null, false, null
        );
        when(ungdomsprogramRegisterKlient.hentForAktørId(AKTØR_ID.getAktørId()))
            .thenReturn(new UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO(List.of(deltakelse)));

        Fagsak fagsak = mock(Fagsak.class);
        when(fagsakTjeneste.finnesEnFagsakSomOverlapper(eq(FagsakYtelseType.UNGDOMSYTELSE), eq(AKTØR_ID), any(), any()))
            .thenReturn(Optional.of(fagsak));

        OrganisasjonsEnhet organisasjonsEnhet = new OrganisasjonsEnhet("0100", "NAV");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak)).thenReturn(organisasjonsEnhet);

        when(pdfGenKlient.lagDokument(anyString(), anyString(), any(), anyBoolean()))
            .thenReturn(new PdfGenDokument(new byte[0], "<html/>"));

        when(dokArkivKlient.opprettJournalpost(any(OpprettJournalpostRequest.class)))
            .thenReturn(new OpprettJournalpostResponse("123", List.of(), true, null));
    }

    @Test
    void opprettJournalpostForInnsendtPapirsøknad_markererDeltakelseSomSøktIUngDeltakelseOpplyser() {
        // Act
        tjeneste.opprettJournalpostForInnsendtPapirsøknad(PersonIdent.fra(FNR), new JournalpostId("123"));

        // Assert: både oppgaven løses og deltakelsen markeres som søkt
        // AktørIdDto (no.nav.ung.brukerdialog.kontrakt) mangler equals(), så vi sammenligner på verdien i stedet for objektet
        ArgumentCaptor<AktørIdDto> aktørIdDtoCaptor = ArgumentCaptor.forClass(AktørIdDto.class);
        verify(oppgaveKlient).løsSøkYtelseOppgave(aktørIdDtoCaptor.capture());
        assertThat(aktørIdDtoCaptor.getValue().getAktorId()).isEqualTo(AKTØR_ID.getAktørId());
        verify(ungdomsprogramRegisterKlient).markerSomSøkt(AKTØR_ID.getAktørId(), DELTAKELSE_ID);
    }

    @Test
    void opprettJournalpostForInnsendtPapirsøknad_løserOppgaveOgMarkererSøktFørJournalpostOpprettes() {
        // Act
        tjeneste.opprettJournalpostForInnsendtPapirsøknad(PersonIdent.fra(FNR), new JournalpostId("123"));

        // Assert: begge kallene skjer før journalposten opprettes
        InOrder inOrder = inOrder(oppgaveKlient, ungdomsprogramRegisterKlient, dokArkivKlient);
        inOrder.verify(oppgaveKlient).løsSøkYtelseOppgave(any());
        inOrder.verify(ungdomsprogramRegisterKlient).markerSomSøkt(anyString(), any());
        inOrder.verify(dokArkivKlient).opprettJournalpost(any());
    }
}
