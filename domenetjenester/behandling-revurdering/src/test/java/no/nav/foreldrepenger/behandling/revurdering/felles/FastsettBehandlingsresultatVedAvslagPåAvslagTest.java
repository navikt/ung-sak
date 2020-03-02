package no.nav.foreldrepenger.behandling.revurdering.felles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class FastsettBehandlingsresultatVedAvslagPåAvslagTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private VilkårResultatRepository vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    private Behandling originalBehandling;
    private Behandling revurdering;

    @Before
    public void setUp() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        originalBehandling = scenario.lagre(repositoryProvider);
        originalBehandling.avsluttBehandling();
        revurdering = lagRevurdering(originalBehandling);
    }

    @Test
    public void skal_ikke_gi_avslag_på_avslag() {
        // Arrange

        // Act
        boolean erAvslagPåAvslag = FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(
            lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING, KonsekvensForYtelsen.INGEN_ENDRING),
            lagBehandlingsresultat(originalBehandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT),
            BehandlingType.FØRSTEGANGSSØKNAD);

        // Assert
        assertThat(erAvslagPåAvslag).isFalse();
    }

    private Behandling lagRevurdering(Behandling originalBehandling) {
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                    .medManueltOpprettet(true)
                    .medOriginalBehandling(originalBehandling))
            .build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling, BehandlingResultatType resultatType, KonsekvensForYtelsen konsekvensForYtelsen) {
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(resultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen).buildFor(behandling);

        final var build = Vilkårene.builder().build();
        vilkårResultatRepository.lagre(behandling.getId(), build);

        return Optional.of(behandlingsresultat);
    }
}
