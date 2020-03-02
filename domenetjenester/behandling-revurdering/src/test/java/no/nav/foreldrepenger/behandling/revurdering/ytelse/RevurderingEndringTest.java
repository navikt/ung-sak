package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;

public class RevurderingEndringTest {

    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private RevurderingEndring revurderingEndring = new no.nav.foreldrepenger.behandling.revurdering.ytelse.RevurderingEndring();
    private Behandling originalBehandling;
    private Behandling revurdering;

    @Before
    public void setup() {
        originalBehandling = opprettOriginalBehandling();
        revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
    }

    @Test
    public void jaHvisRevurderingMedUendretUtfall() {
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        var ref = BehandlingReferanse.fra(revurdering);
        var konsekvenserForYtelsen = Set.of(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(ref, konsekvenserForYtelsen, BehandlingResultatType.INGEN_ENDRING)).isTrue();
        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(ref, konsekvenserForYtelsen, null)).isTrue();
    }

    @Test()
    public void kasterFeilHvisRevurderingMedUendretUtfallOgOpphørAvYtelsen() {

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Assert
        var ref = BehandlingReferanse.fra(revurdering);
        var konsekvenserForYtelsen = Set.of(KonsekvensForYtelsen.INGEN_ENDRING, KonsekvensForYtelsen.YTELSE_OPPHØRER);

        Assert.assertThrows(IllegalStateException.class, () -> {
            assertThat(revurderingEndring.erRevurderingMedUendretUtfall(ref, konsekvenserForYtelsen, BehandlingResultatType.INNVILGET)).isFalse();
        });

    }

    @Test
    public void neiHvisRevurderingMedEndring() {
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        var ref = BehandlingReferanse.fra(revurdering);
        var konsekvenserForYtelsen = Set.of(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK);
        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(ref, konsekvenserForYtelsen, BehandlingResultatType.INNVILGET_ENDRING)).isFalse();

    }

    @Test
    public void neiHvisRevurderingMedOpphør() {
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        var ref = BehandlingReferanse.fra(revurdering);
        var konsekvenserForYtelsen = Set.of(KonsekvensForYtelsen.YTELSE_OPPHØRER);
        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(ref, konsekvenserForYtelsen, BehandlingResultatType.OPPHØR)).isFalse();
    }

    @Test
    public void neiHvisFørstegangsbehandling() {
        var ref = BehandlingReferanse.fra(originalBehandling);
        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(ref, Set.of())).isFalse();
    }

    private Behandling opprettOriginalBehandling() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling originalBehandling = scenario.lagre(repositoryProvider);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(originalBehandling);
        behandlingRepository.lagre(originalBehandling, behandlingLås);
        return originalBehandling;
    }
}
