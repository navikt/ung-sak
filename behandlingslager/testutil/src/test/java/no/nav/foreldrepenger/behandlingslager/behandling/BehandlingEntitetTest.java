package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingEntitetTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Before
    public void setup() {
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
