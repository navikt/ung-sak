package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
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
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ForvaltningMottattDokumentRestTjenesteTest {

    private static final String JOURNALPOST_ID = "999001";
    private static final Saksnummer SAKSNUMMER = new Saksnummer("123456789");
    private static final String FEILMELDING = "Jira sak: ABCD-1234";

    @Inject
    private EntityManager entityManager;

    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private ForvaltningMottattDokumentRestTjeneste tjeneste;

    private final Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE).medSaksnummer(SAKSNUMMER).build();
    private Behandling behandling;

    @BeforeEach
    void setup() {
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);

        tjeneste = new ForvaltningMottattDokumentRestTjeneste(
            fagsakRepository,
            mottatteDokumentRepository,
            entityManager
        );

        fagsakRepository.opprettNy(fagsak);

        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    void skal_sette_mottatt_dokument_til_ugyldig_og_logge_diagnostikk() {
        // Arrange
        var dokumentId = lagreMottattDokument();

        var request = lagRequest();

        // Act
        Response response = tjeneste.markerMottattDokumentUgyldig(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        entityManager.flush();
        entityManager.clear();

        var oppdatert = mottatteDokumentRepository.hentMottattDokument(dokumentId).orElseThrow();
        assertThat(oppdatert.getStatus()).isEqualTo(DokumentStatus.UGYLDIG);
        assertThat(oppdatert.getFeilmelding()).isEqualTo("Manuelt markert som ugyldig. Begrunnelse: " + FEILMELDING);

        Long antallDiagnostikk = entityManager.createQuery(
                "select count(d) from DiagnostikkFagsakLogg d where d.fagsakId = :fagsakId", Long.class)
            .setParameter("fagsakId", fagsak.getId())
            .getSingleResult();
        assertThat(antallDiagnostikk).isEqualTo(1L);
    }

    @Test
    void skal_sette_inntektrapportering_dokument_til_ugyldig() {
        // Arrange
        var dokument = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(JOURNALPOST_ID))
            .medType(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling.getId())
            .build();
        var dokumentId = mottatteDokumentRepository.lagre(dokument, DokumentStatus.MOTTATT).getId();

        var request = lagRequest();

        // Act
        Response response = tjeneste.markerMottattDokumentUgyldig(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        entityManager.flush();
        entityManager.clear();

        var oppdatert = mottatteDokumentRepository.hentMottattDokument(dokumentId).orElseThrow();
        assertThat(oppdatert.getStatus()).isEqualTo(DokumentStatus.UGYLDIG);
        assertThat(oppdatert.getFeilmelding()).isEqualTo("Manuelt markert som ugyldig. Begrunnelse: " + FEILMELDING);
    }

    @Test
    void skal_returnere_400_naar_brevkode_er_feil() {
        // Arrange
        var feilBrevkodeDokument = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(JOURNALPOST_ID))
            .medType(Brevkode.UNGDOMSYTELSE_SOKNAD)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling.getId())
            .build();
        mottatteDokumentRepository.lagre(feilBrevkodeDokument, DokumentStatus.MOTTATT);

        var request = lagRequest();

        // Act
        Response response = tjeneste.markerMottattDokumentUgyldig(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void skal_returnere_400_naar_status_ikke_er_mottatt() {
        // Arrange - dokument med riktig brevkode, men status GYLDIG (ikke MOTTATT)
        var dokument = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(JOURNALPOST_ID))
            .medType(Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling.getId())
            .build();
        mottatteDokumentRepository.lagre(dokument, DokumentStatus.GYLDIG);

        var request = lagRequest();

        // Act
        Response response = tjeneste.markerMottattDokumentUgyldig(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    // --- helpers ---

    private Long lagreMottattDokument() {
        var dokument = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(JOURNALPOST_ID))
            .medType(Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling.getId())
            .build();
        return mottatteDokumentRepository.lagre(dokument, DokumentStatus.MOTTATT).getId();
    }

    private ForvaltningMottattDokumentRestTjeneste.MarkerDokumentUgyldigRequest lagRequest() {
        var journalpostIdDto = new JournalpostId(JOURNALPOST_ID);

        return new ForvaltningMottattDokumentRestTjeneste.MarkerDokumentUgyldigRequest(
            journalpostIdDto,
            SAKSNUMMER,
            new KortTekst(FEILMELDING)
        );
    }
}
