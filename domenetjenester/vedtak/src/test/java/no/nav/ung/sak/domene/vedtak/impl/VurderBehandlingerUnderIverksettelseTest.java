package no.nav.ung.sak.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class VurderBehandlingerUnderIverksettelseTest {

    @Inject
    private EntityManager entityManager;


    private BehandlingRepositoryProvider repositoryProvider ;
    private BehandlingRepository behandlingRepository ;
    private BehandlingVedtakRepository behandlingVedtakRepository ;
    private VilkårResultatRepository vilkårResultatRepository ;
    private VurderBehandlingerUnderIverksettelse tjeneste ;

    private Behandling førstegangBehandling;

    @BeforeEach
    public void setup() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager, behandlingRepository);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        tjeneste = new VurderBehandlingerUnderIverksettelse(repositoryProvider);

        TestScenarioBuilder førstegangScenario = TestScenarioBuilder.builderMedSøknad();
        førstegangBehandling = førstegangScenario.lagre(repositoryProvider);
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

}
