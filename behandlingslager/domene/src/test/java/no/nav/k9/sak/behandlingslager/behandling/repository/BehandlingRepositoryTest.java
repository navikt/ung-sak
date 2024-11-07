package no.nav.k9.sak.behandlingslager.behandling.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.JpaExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingRepository sutRepo;

    @Inject
    private BehandlingRepository behandlingRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    public void test_hentAbsoluttAlleBehandlingerForSaksnummer() throws Exception {
        // Arrange
        var fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.OMSORGSPENGER);

        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak, BehandlingStatus.AVSLUTTET);
        behandlingRepository.lagre(behandling);

        var saksnummer = behandling.getFagsak().getSaksnummer();
        assertThat(saksnummer).isNotNull();

        var behandling2 = behandlingBuilder.opprettNyBehandling(fagsak, BehandlingType.REVURDERING, BehandlingStatus.UTREDES);
        behandlingRepository.lagre(behandling2);

        entityManager.flush();
        entityManager.clear();

        // Act
        var behandlinger = sutRepo.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        // Assert
        assertThat(behandlinger).hasSize(2).map(Behandling::getId).containsOnly(behandling.getId(), behandling2.getId());

        int idx = 0;
        for (var b : behandlinger) {
            assertThat(b.getFagsak()).as("Behandling " + b.getId() + ", idx=" + idx).isNotNull().extracting(Fagsak::getSaksnummer).isEqualTo(saksnummer);
            idx++;
        }

    }

    @Test
    public void test_hentAbsoluttAlleBehandlingerForFagsak() throws Exception {
        // Arrange
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.OMSORGSPENGER, BehandlingStatus.UTREDES);
        behandlingRepository.lagre(behandling);

        var fagsak = behandling.getFagsak();
        var saksnummer = fagsak.getSaksnummer();
        assertThat(fagsak.getId()).isNotNull();
        assertThat(saksnummer).isNotNull();

        entityManager.flush();
        entityManager.clear();

        // Act
        var behandlinger = sutRepo.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId());

        // Assert
        assertThat(behandlinger).hasSize(1).map(Behandling::getId).containsOnly(behandling.getId());
        var beh1 = behandlinger.get(0);
        assertThat(beh1.getFagsak()).isNotNull();
        assertThat(beh1.getFagsak().getSaksnummer()).isEqualTo(saksnummer);

    }
}

