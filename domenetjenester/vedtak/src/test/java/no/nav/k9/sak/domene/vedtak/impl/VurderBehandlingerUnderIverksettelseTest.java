package no.nav.k9.sak.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

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
