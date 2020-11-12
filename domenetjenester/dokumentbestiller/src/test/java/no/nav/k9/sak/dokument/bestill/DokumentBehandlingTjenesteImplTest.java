package no.nav.k9.sak.dokument.bestill;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DokumentBehandlingTjenesteImplTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;

    @Mock
    BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AbstractTestScenario<?> scenario;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private Behandling behandling;
    private BehandlingRepository behandlingRepository;
    private int fristUker = 6;
    private LocalDate now = LocalDate.now();

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        MockitoAnnotations.initMocks(this);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        dokumentBehandlingTjeneste = new DokumentBehandlingTjeneste(behandlingRepository, behandlingskontrollTjeneste);
        this.scenario = TestScenarioBuilder.builderMedSøknad();
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuel() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.finnNyFristManuelt(behandling))
            .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_ingen_terminbekreftelse() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling))
            .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse() {
        this.scenario = TestScenarioBuilder.builderMedSøknad();
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling))
            .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse_i_fortiden() {
        this.scenario = TestScenarioBuilder
            .builderMedSøknad();
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling))
            .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    private void lagBehandling() {
        behandling = Mockito.spy(scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider));
    }

    @Test
    public void skal_lagre_ny_frist() {
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.oppdaterBehandlingMedNyFrist(behandling, now);
        assertThat(behandlingRepository.hentBehandling(behandling.getId()).getBehandlingstidFrist()).isEqualTo(now);
    }
}
