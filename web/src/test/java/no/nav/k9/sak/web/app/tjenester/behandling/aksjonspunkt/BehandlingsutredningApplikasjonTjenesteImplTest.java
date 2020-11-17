package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.Period;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.k9.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.web.app.tjenester.behandling.SjekkProsessering;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BehandlingsutredningApplikasjonTjenesteImplTest {

    @Inject
    private EntityManager entityManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Inject
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingskontrollServiceProvider behandlingskontrollServiceProvider;

    @Mock
    private OppgaveTjeneste oppgaveTjenesteMock;

    @Mock
    private BehandlingModellRepository behandlingModellRepositoryMock;

    @Mock
    private RevurderingTjeneste revurderingTjenesteMock;

    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Mock
    private SjekkProsessering sjekkProsessering;

    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;

    private Long behandlingId;

    @BeforeEach
    public void setUp() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling behandling = TestScenarioBuilder
            .builderMedSøknad()
            .lagre(repositoryProvider);
        behandlingId = behandling.getId();

        BehandlingskontrollTjenesteImpl behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(behandlingskontrollServiceProvider);

        behandlingsutredningApplikasjonTjeneste = new BehandlingsutredningApplikasjonTjeneste(
            Period.parse("P4W"),
            repositoryProvider,
            oppgaveTjenesteMock,
            behandlendeEnhetTjeneste,
            sjekkProsessering,
            behandlingskontrollTjenesteImpl);
    }

    @Test
    public void skal_sette_behandling_pa_vent() {
        // Act
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK, null);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(behandling.getÅpneAksjonspunkter().get(0)).isExactlyInstanceOf(Aksjonspunkt.class);
    }

    @Test
    public void skal_oppdatere_ventefrist_og_arsakskode() {
        // Arrange
        LocalDate toUkerFrem = LocalDate.now().plusWeeks(2);

        // Act
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK, null);
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.AVV_FODSEL, null);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.getFristDatoBehandlingPåVent()).isEqualTo(toUkerFrem);
        assertThat(behandling.getVenteårsak()).isEqualTo(Venteårsak.AVV_FODSEL);
    }

    @Test
    public void skal_kaste_feil_når_oppdatering_av_ventefrist_av_behandling_som_ikke_er_på_vent() {
        Assertions.assertThrows(Exception.class, () -> {
            // Arrange
            LocalDate toUkerFrem = LocalDate.now().plusWeeks(2);

            // Act
            behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.AVV_FODSEL, null);
        });
    }

    @Test
    public void skal_sette_behandling_med_oppgave_pa_vent_og_opprette_task_avslutt_oppgave() {
        // Arrange
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK_VL, "1",
            behandling.getFagsak().getSaksnummer(), behandling);
        oppgaveBehandlingKoblingRepository.lagre(oppgave);

        // Act
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK, null);

        // Assert
        verify(oppgaveTjenesteMock).opprettTaskAvsluttOppgave(any(Behandling.class));
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(behandling.getÅpneAksjonspunkter().get(0)).isExactlyInstanceOf(Aksjonspunkt.class);
    }

    @Test
    public void skal_bytte_behandlende_enhet() {
        // Arrange
        String enhetNavn = "OSLO";
        String enhetId = "22";
        String årsak = "Test begrunnelse";
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet(enhetId, enhetNavn);

        // Act
        behandlingsutredningApplikasjonTjeneste.byttBehandlendeEnhet(behandlingId, enhet, årsak, HistorikkAktør.SAKSBEHANDLER);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        verify(behandlendeEnhetTjeneste).oppdaterBehandlendeEnhet(behandling, enhet, HistorikkAktør.SAKSBEHANDLER, årsak);
    }
}
