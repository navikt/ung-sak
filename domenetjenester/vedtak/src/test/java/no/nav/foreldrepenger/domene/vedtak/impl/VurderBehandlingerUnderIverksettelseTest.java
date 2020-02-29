package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class VurderBehandlingerUnderIverksettelseTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final Repository repository = repoRule.getRepository();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingVedtakRepository behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager, behandlingRepository);
    private final VilkårResultatRepository vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();

    private final VurderBehandlingerUnderIverksettelse tjeneste = new VurderBehandlingerUnderIverksettelse(repositoryProvider);

    private Behandling førstegangBehandling;

    @Before
    public void setup() {
        TestScenarioBuilder førstegangScenario = TestScenarioBuilder.builderMedSøknad();
        førstegangScenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        førstegangBehandling = førstegangScenario.lagre(repositoryProvider);
        repository.lagre(getBehandlingsresultat(førstegangBehandling));
    }

    @Test
    public void neiHvisIngenAnnenBehandling() {
        // Act
        boolean resultat = tjeneste.vurder(førstegangBehandling);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void neiHvisAnnenBehandlingErIverksatt() {
        // Arrange
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IVERKSATT);
        Behandling revurdering = lagreRevurdering();
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        boolean resultat = tjeneste.vurder(revurdering);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void jaHvisAnnenBehandlingErIkkeIverksatt() {
        // Arrange
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IKKE_IVERKSATT);
        Behandling revurdering = lagreRevurdering();
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        boolean resultat = tjeneste.vurder(revurdering);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void neiForFørstegangsbehandlingNårRevurderingErUnderIverksetting() {
        // Arrange
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IKKE_IVERKSATT);
        Behandling revurdering = lagreRevurdering();
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        boolean resultat = tjeneste.vurder(førstegangBehandling);

        // Assert
        assertThat(resultat).isFalse();
    }

    private Behandling lagreRevurdering() {
        Behandling revurdering = Behandling.fraTidligereBehandling(førstegangBehandling, BehandlingType.REVURDERING).build();
        BehandlingLås lås = new BehandlingLås(revurdering.getId());
        behandlingRepository.lagre(revurdering, lås);
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().buildFor(revurdering);
        repository.lagre(behandlingsresultat);
        vilkårResultatRepository.lagre(revurdering.getId(), Vilkårene.builder().build());
        return revurdering;
    }

    private BehandlingVedtak lagreBehandlingVedtak(Behandling behandling, IverksettingStatus iverksettingStatus) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medIverksettingStatus(iverksettingStatus)
            .build();
        LocalDateTime opprettetTidspunkt = behandling.erRevurdering() ? LocalDateTime.now().plusSeconds(1) : LocalDateTime.now();
        Whitebox.setInternalState(behandlingVedtak, "opprettetTidspunkt", opprettetTidspunkt);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        if (IverksettingStatus.IKKE_IVERKSATT.equals(iverksettingStatus)) {
            Whitebox.setInternalState(behandling, "status", BehandlingStatus.IVERKSETTER_VEDTAK);
        }
        return behandlingVedtak;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }
}
