package no.nav.k9.sak.domene.vedtak.intern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTaskProperties;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.k9.sak.domene.vedtak.intern.AvsluttBehandling;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;

@SuppressWarnings("deprecation")
public class AvsluttBehandlingTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Mock
    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    private AvsluttBehandling avsluttBehandling;
    private Behandling behandling;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    private Fagsak fagsak;

    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setUp() {
        behandling = lagBehandling(LocalDateTime.now().minusHours(1), LocalDateTime.now());
        fagsak = behandling.getFagsak();
        behandlingReferanse = BehandlingReferanse.fra(behandling);

        vurderBehandlingerUnderIverksettelse = new VurderBehandlingerUnderIverksettelse(repositoryProvider);

        avsluttBehandling = new AvsluttBehandling(repositoryProvider, behandlingskontrollTjeneste,
            behandlingVedtakEventPubliserer, vurderBehandlingerUnderIverksettelse, prosessTaskRepository);

        when(behandlingskontrollTjeneste.initBehandlingskontroll(Mockito.anyLong())).thenAnswer(invocation -> {
            Long behId = invocation.getArgument(0);
            BehandlingLås lås = new BehandlingLås(behId) {
            };
            return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);
        });
        when(behandlingskontrollTjeneste.initBehandlingskontroll(Mockito.any(Behandling.class)))
            .thenAnswer(invocation -> {
                Behandling beh = invocation.getArgument(0);
                BehandlingLås lås = new BehandlingLås(beh.getId()) {
                };
                return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);
            });
    }

    @Test
    public void testAvsluttBehandlingUtenAndreBehandlingerISaken() {
        // Arrange
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(any())).thenReturn(Collections.singletonList(behandling));

        // Act
        avsluttBehandling();

        // Assert
        verifiserIverksatt();
        verifiserKallTilProsesserBehandling(behandling);
    }

    private void verifiserIverksatt() {
        var argCapture = ArgumentCaptor.forClass(BehandlingVedtak.class);
        verify(repositoryProvider.getBehandlingVedtakRepository()).lagre(argCapture.capture(), any(BehandlingLås.class));
        var vedtak = argCapture.getValue();
        verify(vedtak).setIverksettingStatus(IverksettingStatus.IVERKSATT);

    }

    @Test
    public void testAvsluttBehandlingMedAnnenBehandlingSomIkkeVenter() {
        // Arrange
        Behandling behandling2 = TestScenarioBuilder.builderMedSøknad().lagMocked();
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(List.of(behandling, behandling2));

        // Act
        avsluttBehandling();

        verifiserIverksatt();
        verifiserKallTilProsesserBehandling(behandling);
        verify(prosessTaskRepository, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void testAvsluttBehandlingMedAnnenBehandlingSomVenter() {
        // Arrange
        Behandling annenBehandling = lagBehandling(LocalDateTime.now().minusDays(1), LocalDateTime.now());
        BehandlingStegTilstand tilstand = new BehandlingStegTilstand(annenBehandling, BehandlingStegType.IVERKSETT_VEDTAK, BehandlingStegStatus.STARTET);
        Whitebox.setInternalState(annenBehandling, "status", BehandlingStatus.IVERKSETTER_VEDTAK);
        Whitebox.setInternalState(annenBehandling, "behandlingStegTilstander", List.of(tilstand));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(List.of(behandling, annenBehandling));

        // Act
        avsluttBehandling();

        verifiserIverksatt();
        verifiserKallTilProsesserBehandling(behandling);
        verifiserKallTilFortsettBehandling(annenBehandling);
    }

    @Test
    public void testAvsluttBehandlingMedAnnenBehandlingSomErUnderIverksetting() {
        // Arrange
        Behandling annenBehandling = lagBehandling(LocalDateTime.now().minusDays(1), LocalDateTime.now());
        BehandlingStegTilstand tilstand = new BehandlingStegTilstand(annenBehandling, BehandlingStegType.IVERKSETT_VEDTAK, BehandlingStegStatus.VENTER);
        Whitebox.setInternalState(annenBehandling, "status", BehandlingStatus.IVERKSETTER_VEDTAK);
        Whitebox.setInternalState(annenBehandling, "behandlingStegTilstander", List.of(tilstand));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(List.of(behandling, annenBehandling));

        // Act
        avsluttBehandling();

        verifiserIverksatt();
        verifiserKallTilProsesserBehandling(behandling);
        verify(prosessTaskRepository, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void testAvsluttBehandlingMedToAndreBehandlingerSomVenterEldsteFørst() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Behandling annenBehandling = lagBehandling(now.minusDays(2), now);
        Behandling tredjeBehandling = lagBehandling(now, now);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId()))
            .thenReturn(List.of(behandling, annenBehandling, tredjeBehandling));

        // Act
        avsluttBehandling();

        verifiserIverksatt();
        verifiserKallTilProsesserBehandling(behandling);
        verifiserKallTilFortsettBehandling(annenBehandling);
        verifiserIkkeKallTilFortsettBehandling(tredjeBehandling);
    }

    private void verifiserKallTilProsesserBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        verify(behandlingskontrollTjeneste).prosesserBehandlingGjenopptaHvisStegVenter(kontekst, BehandlingStegType.IVERKSETT_VEDTAK);
    }

    private void verifiserKallTilFortsettBehandling(Behandling behandling) {
        ArgumentCaptor<ProsessTaskData> prosessTaskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository).lagre(prosessTaskCaptor.capture());
        List<ProsessTaskData> arguments = prosessTaskCaptor.getAllValues();

        assertThat(inneholderFortsettBehandlingTaskForBehandling(arguments, behandling)).isTrue();
    }

    private void verifiserIkkeKallTilFortsettBehandling(Behandling behandling) {
        ArgumentCaptor<ProsessTaskData> prosessTaskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository).lagre(prosessTaskCaptor.capture());
        List<ProsessTaskData> arguments = prosessTaskCaptor.getAllValues();

        assertThat(inneholderFortsettBehandlingTaskForBehandling(arguments, behandling)).isFalse();
    }

    private boolean inneholderFortsettBehandlingTaskForBehandling(List<ProsessTaskData> arguments, Behandling behandling) {
        return arguments.stream()
            .anyMatch(argument -> argument.getTaskType().equals(FortsettBehandlingTaskProperties.TASKTYPE)
                && argument.getBehandlingId().equals(String.valueOf(behandling.getId())));
    }

    @Test
    public void testAvsluttBehandlingMedToAndreBehandlingerSomVenterEldsteSist() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Behandling annenBehandling = lagBehandling(now, now);
        Behandling tredjeBehandling = lagBehandling(now.minusDays(1), now);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId()))
            .thenReturn(List.of(behandling, annenBehandling, tredjeBehandling));

        // Act
        avsluttBehandling();

        verifiserIverksatt();
        verifiserKallTilProsesserBehandling(behandling);
        verifiserKallTilFortsettBehandling(tredjeBehandling);
        verifiserIkkeKallTilFortsettBehandling(annenBehandling);
    }

    private void avsluttBehandling() {
        avsluttBehandling.avsluttBehandling(behandlingReferanse);
    }

    private Behandling lagBehandling(LocalDateTime opprettet, LocalDateTime vedtaksdato) {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);

        if (fagsak != null) {
            scenario.medFagsakId(fagsak.getId());
            scenario.medSaksnummer(fagsak.getSaksnummer());
        }
        if (repositoryProvider == null) {
            repositoryProvider = scenario.mockBehandlingRepositoryProvider();
            behandlingRepository = repositoryProvider.getBehandlingRepository();
        }
        Behandling behandling = scenario.lagMocked();

        when(behandlingRepository.hentBehandlingHvisFinnes(behandling.getId())).thenReturn(Optional.of(behandling));

        if (vedtaksdato != null) {
            BehandlingVedtak vedtak = lagMockedBehandlingVedtak(opprettet, vedtaksdato, behandling);
            when(repositoryProvider.getBehandlingVedtakRepository().hentBehandlingVedtakForBehandlingId(behandling.getId())).thenReturn(Optional.of(vedtak));
            Whitebox.setInternalState(behandling, "avsluttetDato", vedtaksdato);
        }
        Whitebox.setInternalState(behandling, "status", BehandlingStatus.IVERKSETTER_VEDTAK);
        Whitebox.setInternalState(behandling, "behandlingStegTilstander",
            List.of(new BehandlingStegTilstand(behandling, BehandlingStegType.IVERKSETT_VEDTAK, BehandlingStegStatus.STARTET)));
        return behandling;
    }

    private BehandlingVedtak lagMockedBehandlingVedtak(LocalDateTime opprettet, LocalDateTime vedtaksdato, Behandling behandling) {
        BehandlingVedtak vedtak = Mockito.spy(BehandlingVedtak.builder(behandling.getId())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Severin Saksbehandler")
            .medIverksettingStatus(IverksettingStatus.IKKE_IVERKSATT)
            .medVedtakstidspunkt(vedtaksdato).build());
        Whitebox.setInternalState(vedtak, "opprettetTidspunkt", opprettet);
        return vedtak;
    }

}
