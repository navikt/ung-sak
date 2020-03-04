package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingVedtakRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private Behandling behandling;

    private BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(entityManager);

    @Before
    public void setup() {
        behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void skalLagreVedtak() {
        // Arrange
        BehandlingVedtak behandlingVedtak = opprettBehandlingVedtak(behandling);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);

        // Assert
        Long behandlingVedtakId = behandlingVedtak.getId();
        assertThat(behandlingVedtakId).isNotNull();
        BehandlingVedtak lagret = repository.hent(BehandlingVedtak.class, behandlingVedtakId);
        assertThat(lagret).isSameAs(behandlingVedtak);
    }

    @Test
    public void skalLagreOgHenteVedtak() {
        // Arrange
        BehandlingVedtak behandlingVedtak = opprettBehandlingVedtak(behandling);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        Optional<BehandlingVedtak> lagretVedtakOpt = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId());

        // Assert
        assertThat(lagretVedtakOpt).hasValueSatisfying(lagretVedtak -> {
            assertThat(lagretVedtak.getId()).isNotNull();
            assertThat(lagretVedtak).isSameAs(behandlingVedtak);
        });
    }

    private BehandlingVedtak opprettBehandlingVedtak(Behandling behandling) {
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .build();
        return behandlingVedtak;
    }
}
