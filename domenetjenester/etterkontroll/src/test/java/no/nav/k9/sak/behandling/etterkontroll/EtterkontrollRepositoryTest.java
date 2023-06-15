package no.nav.k9.sak.behandling.etterkontroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class EtterkontrollRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private EtterkontrollRepository etterkontrollRepository;

    @BeforeEach
    public void before() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void skal_hente_aktive_etterkontroller_etter_kontrollfrist() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll1 = new Etterkontroll
            .Builder(behandling)
            .medErBehandlet(false)
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay())
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll1);

        Etterkontroll etterkontroll2 = new Etterkontroll
            .Builder(behandling)
            .medErBehandlet(true)
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay())
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll2);

        final List<Etterkontroll> etterkontroller = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll();

        Assertions.assertThat(etterkontroller).hasSize(1);
        assertThat(etterkontroller.get(0).getFagsakId()).isEqualTo(behandling.getFagsakId());
    }

    @Test
    public void skal_ikke_hente_etterkontroller_før_kontrollfrist() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling)
            .medErBehandlet(false)
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay().plusDays(5))
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll);

        List<Etterkontroll> fagsakList = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll();

        Assertions.assertThat(fagsakList).isEmpty();
    }


    @Test
    public void skal_ikke_tillate_mer_enn_en_aktiv_etterkontroll_per_behandling_og_kontrollType() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll1 = new Etterkontroll.Builder(behandling)
            .medErBehandlet(false)
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay())
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll1);

        Etterkontroll etterkontroll2 = new Etterkontroll.Builder(behandling)
            .medErBehandlet(false)
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay())
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();

        assertThatThrownBy(() -> etterkontrollRepository.lagre(etterkontroll2))
            .isInstanceOf(PersistenceException.class);

    }


    private Behandling opprettRevurderingsKandidat() {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medAnsvarligSaksbehandler("asdf");
        Behandling behandling = scenario.lagre(repositoryProvider);

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

}
