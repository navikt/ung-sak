package no.nav.ung.sak.mottak.dokumentmottak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.JournalpostId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class MottatteDokumentRepositoryTest {

    private String payload = "en payload";

    @Inject
    private EntityManager entityManager;

    private MottatteDokumentRepository mottatteDokumentRepository ;
    private BehandlingRepository behandlingRepository ;
    private FagsakRepository fagsakRepository ;

    private final Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).build();
    private Behandling beh1, beh2;
    private MottattDokument dokument1, dokument2;

    private long journalpostId = 123L;

    @BeforeEach
    public void setup() {

        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);

        fagsakRepository.opprettNy(fagsak);

        beh1 = opprettBuilderForBehandling().build();
        lagreBehandling(beh1);

        beh2 = opprettBuilderForBehandling().build();
        lagreBehandling(beh2);

        // Opprett og lagre MottateDokument
        dokument1 = lagMottatteDokument(beh1, Brevkode.UNGDOMSYTELSE_SOKNAD, payload);
        mottatteDokumentRepository.lagre(dokument1, DokumentStatus.GYLDIG);

        // Dokument knyttet til annen behandling, men med samme fagsak som dokumentet over
        dokument2 = lagMottatteDokument(beh2, Brevkode.UNGDOMSYTELSE_SOKNAD, payload);
        mottatteDokumentRepository.lagre(dokument2, DokumentStatus.GYLDIG);
    }

    @Test
    public void skal_hente_alle_MottatteDokument_på_fagsakId() {
        // Act
        List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(fagsak.getId());

        // Assert
        assertThat(mottatteDokumenter).hasSize(2);
    }

    @Test
    public void skal_hente_MottattDokument_på_id() {
        // Act
        Optional<MottattDokument> mottattDokument1 = mottatteDokumentRepository.hentMottattDokument(dokument1.getId());
        Optional<MottattDokument> mottattDokument2 = mottatteDokumentRepository.hentMottattDokument(dokument2.getId());

        // Assert
        assertThat(dokument1).isEqualTo(mottattDokument1.get());
        assertThat(dokument2).isEqualTo(mottattDokument2.get());
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    public MottattDokument lagMottatteDokument(Behandling beh, Brevkode type, String payload) {
        return new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(journalpostId++))
            .medType(type)
            .medMottattDato(LocalDate.now())
            .medFagsakId(beh.getFagsakId())
            .medBehandlingId(beh.getId())
            .medPayload(payload)
            .build();
    }

    private Behandling.Builder opprettBuilderForBehandling() {
        return Behandling.forFørstegangssøknad(fagsak);

    }

}
