package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingVedtakRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private BehandlingRepositoryProvider repositoryProvider ;

    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;

    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
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
