package no.nav.k9.sak.dokument.bestill;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.dokument.bestill.DokumentBehandlingTjeneste;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class DokumentBehandlingTjenesteImplTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Mock
    BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AbstractTestScenario<?> scenario;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private Behandling behandling;
    private BehandlingRepository behandlingRepository;
    private int fristUker = 6;
    private LocalDate now = LocalDate.now();

    @Before
    public void setUp() {
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
