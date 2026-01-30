package no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.web.app.tjenester.behandling.SjekkProsessering;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BehandlingsutredningApplikasjonTjenesteImplTest {

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
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Mock
    private SjekkProsessering sjekkProsessering;

    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;

    private Long behandlingId;
    private EtterlysningRepository etterlysningRepository;

    @BeforeEach
    public void setUp() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling behandling = TestScenarioBuilder
            .builderMedSøknad()
            .lagre(repositoryProvider);
        behandlingId = behandling.getId();

        BehandlingskontrollTjenesteImpl behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(behandlingskontrollServiceProvider);

        etterlysningRepository = new EtterlysningRepository(repositoryProvider.getEntityManager());
        behandlingsutredningApplikasjonTjeneste = new BehandlingsutredningApplikasjonTjeneste(
            Period.parse("P4W"),
            repositoryProvider,
            oppgaveTjenesteMock,
            etterlysningRepository,
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
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.VENTER_SVAR_TEAMS, null);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.getFristDatoBehandlingPåVent()).isEqualTo(toUkerFrem);
        assertThat(behandling.getVenteårsak()).isEqualTo(Venteårsak.VENTER_SVAR_TEAMS);
    }

    @Test
    public void skal_oppdatere_ventefrist_ved_endring_av_etterlysning() {
        // Arrange
        var gammelFrist = LocalDateTime.now();
        var nyFrist = gammelFrist.plusWeeks(2);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(gammelFrist.toLocalDate().minusMonths(1).withDayOfMonth(1), gammelFrist.toLocalDate().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
        Etterlysning etterlysning = new Etterlysning(behandlingId, UUID.randomUUID(), UUID.randomUUID(), periode, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT, EtterlysningStatus.OPPRETTET);
        etterlysning.vent(nyFrist);
        etterlysningRepository.lagre(etterlysning);

        AksjonspunktKontrollRepository aksjonspunktKontrollRepository = behandlingskontrollServiceProvider.getAksjonspunktKontrollRepository();
        aksjonspunktKontrollRepository.settBehandlingPåVent(behandlingRepository.hentBehandling(behandlingId), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.tilAutopunktDefinisjon(),
            BehandlingStegType.VURDER_KOMPLETTHET, gammelFrist, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.mapTilVenteårsak(), null);


        // Act
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(behandlingId, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.getFristDatoBehandlingPåVent()).isEqualTo(nyFrist.toLocalDate());
        assertThat(behandling.getVenteårsak()).isEqualTo(EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.mapTilVenteårsak());
    }

    @Test
    public void skal_kaste_feil_når_oppdatering_av_ventefrist_av_behandling_som_ikke_er_på_vent() {
        Assertions.assertThrows(Exception.class, () -> {
            // Arrange
            LocalDate toUkerFrem = LocalDate.now().plusWeeks(2);

            // Act
            behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.VENTER_SVAR_TEAMS, null);
        });
    }

    @Test
    public void skal_sette_behandling_med_oppgave_pa_vent_og_opprette_task_avslutt_oppgave() {
        // Arrange
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "1",
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
