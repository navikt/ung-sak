package no.nav.ung.sak.behandling.revurdering.etterkontroll.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UtførKontrollTjenesteTest {

    @Inject
    private EntityManager entityManager;
    private final KontrollTjeneste kontrollTjeneste = mock();
    private UtførKontrollTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private EtterkontrollRepository etterkontrollRepository;

    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        etterkontrollRepository = new EtterkontrollRepository(entityManager);

        tjeneste = new UtførKontrollTjeneste(etterkontrollRepository, new UnitTestLookupInstanceImpl<>(kontrollTjeneste));

    }

    @Test
    void utfører_etterkontroller_på_behandling() {
        when(kontrollTjeneste.utfør(any())).thenReturn(true);

        Behandling behandling = opprettBehandling();
        Long etterkontrollId = etterkontrollRepository.lagre(new Etterkontroll.Builder(behandling)
            .medErBehandlet(false)
            .medKontrollTidspunkt(LocalDateTime.now())
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .build());

        tjeneste.utfør(behandling, etterkontrollId.toString());

        var etterkontroll = etterkontrollRepository.hent(etterkontrollId.toString());

        assertThat(etterkontroll.isBehandlet()).isTrue();
    }

    private Behandling opprettBehandling() {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);

        scenario.medBehandlingVedtak().medAnsvarligSaksbehandler("asdf");

        var behandling = scenario.lagre(entityManager);

        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

}
