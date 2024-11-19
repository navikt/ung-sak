package no.nav.ung.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.db.util.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingEntitetTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void skal_opprette_ny_behandling_på_ny_fagsak() {

        Behandling behandling = opprettOgLagreBehandling();

        List<Behandling> alle = repository.hentAlle(Behandling.class);

        assertThat(alle).hasSize(1);

        Behandling første = alle.get(0);

        assertThat(første).isEqualTo(behandling);
    }

    private Behandling opprettOgLagreBehandling() {
        var testScenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        return testScenarioBuilder.lagre(repositoryProvider);
    }

    @Test
    public void skal_opprette_ny_behandling_på_fagsak_med_tidligere_behandling() {

        Behandling behandling = opprettOgLagreBehandling();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        lagreBehandling(behandling2);

        List<Behandling> alle = repository.hentAlle(Behandling.class);

        assertThat(alle).hasSize(2);

        Behandling første = alle.get(0);
        Behandling andre = alle.get(1);

        assertThat(første).isNotEqualTo(andre);
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    @Test
    public void skal_opprette_ny_behandling_med_søknad() {
        var scenario = TestScenarioBuilder.builderMedSøknad();

        scenario.lagre(repositoryProvider);

        List<Behandling> alle = repository.hentAlle(Behandling.class);

        assertThat(alle).hasSize(1);

        Behandling første = alle.get(0);
        final SøknadEntitet søknad = repositoryProvider.getSøknadRepository().hentSøknad(første);
        assertThat(søknad).isNotNull();
        assertThat(søknad.getSøknadsdato()).isEqualTo(LocalDate.now());
    }

}
