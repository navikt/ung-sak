package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretSluttdatoBekreftelse;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.fordel.repo.journalpost.JournalpostMottattEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ForvaltningMottattDokumentRestTjenesteTest {

    private static final String JOURNALPOST_ID = "999001";
    private static final String NY_UTTALELSE = "Jeg ønsker å forlenge perioden";

    // Payload (OppgaveBekreftelse-wrapper) uten uttalelseFraBruker
    private static final String PAYLOAD_UTEN_UTTALELSE = """
        {
          "bekreftelse": {
            "type": "UNG_ENDRET_SLUTTDATO",
            "dataBruktTilUtledning": null,
            "harUttalelse": true,
            "nySluttdato": "2026-05-11",
            "oppgaveReferanse": "13c9da9d-815b-4d67-bfd5-2abba88d2d55",
            "uttalelseFraBruker": null
          },
          "mottattDato": "2026-06-17T10:01:18.283489444Z",
          "språk": "nb",
          "søker": { "norskIdentitetsnummer": "13420086762" },
          "søknadId": "13c9da9d-815b-4d67-bfd5-2abba88d2d55",
          "versjon": "1.0.0",
          "kildesystem": "søknadsdialog"
        }
        """;

    @Inject
    private EntityManager entityManager;

    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private JournalpostRepository journalpostRepository;
    private ForvaltningMottattDokumentRestTjeneste tjeneste;

    private final Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE).build();
    private Behandling behandling;

    @BeforeEach
    void setup() {
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        journalpostRepository = new JournalpostRepository(entityManager);

        tjeneste = new ForvaltningMottattDokumentRestTjeneste(
            behandlingRepository,
            mottatteDokumentRepository,
            entityManager,
            journalpostRepository
        );

        fagsakRepository.opprettNy(fagsak);

        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    void skal_oppdatere_uttalelse_i_mottatt_dokument_og_journalpost_mottatt() {
        // Arrange
        lagreMottattDokument(PAYLOAD_UTEN_UTTALELSE);
        lagreJournalpostMottatt(PAYLOAD_UTEN_UTTALELSE);

        var request = lagRequest(NY_UTTALELSE);

        // Act
        Response response = tjeneste.oppdaterMottattDokument(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        entityManager.flush();
        entityManager.clear();

        var oppdatertDokument = mottatteDokumentRepository.hentMottatteDokument(
            fagsak.getId(), List.of(new JournalpostId(JOURNALPOST_ID))).getFirst();
        assertThat(uttalelseAv(oppdatertDokument.getPayload())).isEqualTo(NY_UTTALELSE);

        var oppdatertJournalpost = journalpostRepository.finnJournalpostMottatt(new JournalpostId(JOURNALPOST_ID));
        assertThat(oppdatertJournalpost).isPresent();
        assertThat(uttalelseAv(oppdatertJournalpost.get().getPayload())).isEqualTo(NY_UTTALELSE);
    }

    @Test
    void skal_returnere_400_naar_ny_uttalelse_mangler() {
        // Arrange
        lagreMottattDokument(PAYLOAD_UTEN_UTTALELSE);

        var request = lagRequest("   ");

        // Act
        Response response = tjeneste.oppdaterMottattDokument(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void skal_returnere_400_naar_eksisterende_payload_allerede_har_uttalelse() {
        // Arrange - lagre dokument der uttalelse allerede er satt
        var medUttalelse = PAYLOAD_UTEN_UTTALELSE.replace("\"uttalelseFraBruker\": null", "\"uttalelseFraBruker\": \"allerede satt\"");
        lagreMottattDokument(medUttalelse);

        var request = lagRequest(NY_UTTALELSE);

        // Act
        Response response = tjeneste.oppdaterMottattDokument(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void skal_returnere_400_naar_brevkode_er_feil() {
        // Arrange
        var feilBrevkodeDokument = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(JOURNALPOST_ID))
            .medType(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling.getId())
            .medPayload(PAYLOAD_UTEN_UTTALELSE)
            .build();
        mottatteDokumentRepository.lagre(feilBrevkodeDokument, DokumentStatus.GYLDIG);

        var request = lagRequest(NY_UTTALELSE);

        // Act
        Response response = tjeneste.oppdaterMottattDokument(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    // --- helpers ---

    private static String uttalelseAv(String payload) {
        OppgaveBekreftelse ob = JsonUtils.fromString(payload, OppgaveBekreftelse.class);
        EndretSluttdatoBekreftelse b = ob.getBekreftelse();
        return b.getUttalelseFraBruker();
    }

    private void lagreMottattDokument(String payload) {
        var dokument = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(JOURNALPOST_ID))
            .medType(Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling.getId())
            .medPayload(payload)
            .build();
        mottatteDokumentRepository.lagre(dokument, DokumentStatus.GYLDIG);
    }

    private void lagreJournalpostMottatt(String payload) {
        var journalpostMottatt = new JournalpostMottattEntitet(
            new JournalpostId(JOURNALPOST_ID),
            null,
            new AktørId(fagsak.getAktørId().getId()),
            Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE.getOffisiellKode(),
            LocalDateTime.now(),
            "Varsel uttalelse",
            payload,
            JournalpostMottattEntitet.Status.UBEHANDLET
        );
        journalpostRepository.lagreMottatt(journalpostMottatt);
    }

    private ForvaltningMottattDokumentRestTjeneste.OppdaterMottattDokumentRequest lagRequest(String nyUttalelse) {
        var journalpostIdDto = new JournalpostId(JOURNALPOST_ID);

        return new ForvaltningMottattDokumentRestTjeneste.OppdaterMottattDokumentRequest(
            journalpostIdDto,
            new BehandlingIdDto(behandling.getId()),
            nyUttalelse
        );
    }
}

