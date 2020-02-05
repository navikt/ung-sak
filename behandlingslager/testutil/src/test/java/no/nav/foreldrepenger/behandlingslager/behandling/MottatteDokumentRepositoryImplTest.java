package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.typer.JournalpostId;

public class MottatteDokumentRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private final MottatteDokumentRepository mottatteDokumentRepository = new MottatteDokumentRepository(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = new BehandlingRepository(repoRule.getEntityManager());
    private final FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());

    private final Fagsak fagsak = FagsakBuilder.nyEngangstønad().build();
    private Behandling beh1, beh2;
    private MottattDokument dokument1, dokument2;

    @Before
    public void setup(){
        fagsakRepository.opprettNy(fagsak);
        
        beh1 = opprettBuilderForBehandling().build();
        lagreBehandling(beh1);

        beh2 = opprettBuilderForBehandling().build();
        lagreBehandling(beh2);

        //Opprett og lagre MottateDokument
        dokument1 = lagMottatteDokument(beh1.getId(), beh1.getFagsakId());
        mottatteDokumentRepository.lagre(dokument1);

        //Dokument knyttet til annen behandling, men med samme fagsak som dokumentet over
        dokument2 = lagMottatteDokument(beh2.getId(), beh2.getFagsakId());
        mottatteDokumentRepository.lagre(dokument2);
    }

    @Test
    public void skal_hente_alle_MottatteDokument_på_behandlingId() {
        //Act
        List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(beh1.getId());

        //Assert
        assertThat(mottatteDokumenter).hasSize(1);
        assertThat(mottatteDokumenter.get(0).getBehandlingId()).isEqualTo(beh1.getId());
    }

    @Test
    public void skal_hente_alle_MottatteDokument_på_fagsakId() {
        //Act
        List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsak.getId());

        //Assert
        assertThat(mottatteDokumenter).hasSize(2);
        assertThat(mottatteDokumenter.stream().allMatch(md -> md.getBehandlingId().equals(beh1.getId()) || md.getBehandlingId().equals(beh2.getId()))).isTrue();
    }

    @Test
    public void skal_hente_MottattDokument_på_id() {
        //Act
        Optional<MottattDokument> mottattDokument1 = mottatteDokumentRepository.hentMottattDokument(dokument1.getId());
        Optional<MottattDokument> mottattDokument2 = mottatteDokumentRepository.hentMottattDokument(dokument2.getId());

        //Assert
        assertThat(dokument1).isEqualTo(mottattDokument1.get());
        assertThat(dokument2).isEqualTo(mottattDokument2.get());
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    public static MottattDokument lagMottatteDokument(long behandlingId, long fagsakId) {
        return new MottattDokument.Builder()
            .medBehandlingId(behandlingId)
            .medJournalPostId(new JournalpostId("123"))
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(true)
            .medFagsakId(fagsakId)
            .build();
    }

    private Behandling.Builder opprettBuilderForBehandling() {
        return Behandling.forFørstegangssøknad(fagsak);

    }

}
