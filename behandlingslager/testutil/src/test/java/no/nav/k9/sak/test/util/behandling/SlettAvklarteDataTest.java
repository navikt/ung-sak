package no.nav.k9.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class SlettAvklarteDataTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private MedlemskapRepository medlemskapRepository;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    @Test
    public void skal_slette_avklarte_medlemskapdata() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        Long behandlingId = behandling.getId();
        medlemskapRepository.slettAvklarteMedlemskapsdata(behandlingId, lås);
        repository.flushAndClear();

        // Assert
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        assertThat(medlemskap).isPresent();

    }
}
