package no.nav.ung.sak.mottak.dokumentmottak;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.mottak.Behandlingsoppretter;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InnhentDokumentTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;

    private AksjonspunktTestSupport aksjonspunktRepository;

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private Dokumentmottaker dokumentmottaker;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @BeforeEach
    public void oppsett() {
        aksjonspunktRepository = new AksjonspunktTestSupport();

        MockitoAnnotations.initMocks(this);

        dokumentmottakerFelles = Mockito.spy(new DokumentmottakerFelles(
            prosessTaskTjeneste,
            historikkinnslagTjeneste));

        innhentDokumentTjeneste = Mockito.spy(new InnhentDokumentTjeneste(
            new UnitTestLookupInstanceImpl<>(dokumentmottaker),
            dokumentmottakerFelles,
            behandlingsoppretter,
            repositoryProvider,
            behandlingProsesseringTjeneste,
            prosessTaskTjeneste,
            fagsakProsessTaskRepository));

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
        when(behandlingProsesseringTjeneste.opprettTaskGruppeForGjenopptaOppdaterFortsett(any(Behandling.class), anyBoolean(), anyBoolean())).thenReturn(new ProsessTaskGruppe());

        when(dokumentmottaker.getBehandlingÅrsakType(Brevkode.INNTEKTSMELDING)).thenReturn(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
    }

    @Test
    public void skal_oppdatere_ukomplett_behandling_med_IM_dersom_fagsak_har_avsluttet_behandling_og_åpen_behandling_og_kompletthet_ikke_passert() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        TestScenarioBuilder revurderingScenario = TestScenarioBuilder.builderMedSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingStegStart(BehandlingStegType.VURDER_UTLAND)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        Behandling revurderingBehandling = revurderingScenario.lagre(repositoryProvider);
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(revurderingBehandling.getFagsakId(), "", now(), "123", Brevkode.INNTEKTSMELDING);

        // Act
        innhentDokumentTjeneste.mottaDokument(revurderingBehandling.getFagsak(), List.of(mottattDokument));

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTaskGruppeForGjenopptaOppdaterFortsett(revurderingBehandling, false, false);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), revurderingBehandling);
    }

    @Test
    public void skal_oppdatere_behandling_vurdere_kompletthet_og_spole_til_nytt_startpunkt_dersom_fagsak_har_avsluttet_behandling_har_åpen_behandling_og_kompletthet_passert() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        // Arrange - opprette revurdering som har passert kompletthet
        TestScenarioBuilder revurderingScenario = TestScenarioBuilder.builderMedSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingStegStart(BehandlingStegType.FORESLÅ_VEDTAK)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        Behandling revurderingBehandling = revurderingScenario.lagre(repositoryProvider);

        // Arrange - bygg dok
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(revurderingBehandling.getFagsakId(), "", now(), "123", Brevkode.INNTEKTSMELDING);

        // Act
        innhentDokumentTjeneste.mottaDokument(revurderingBehandling.getFagsak(), List.of(mottattDokument));

        // Assert - sjekk flyt
        verify(behandlingProsesseringTjeneste).opprettTaskGruppeForGjenopptaOppdaterFortsett(revurderingBehandling, false, false);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), revurderingBehandling);
    }

    @Test
    public void skal_lagre_dokument_og_vurdere_kompletthet_dersom_inntektsmelding_på_åpen_behandling() {
        // Arrange - opprette åpen behandling
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, LocalDateTime.now());

        // Arrange - bygg dok
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", Brevkode.INNTEKTSMELDING);

        // Act
        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(mottattDokument));

        // Assert - sjekk flyt
        verify(behandlingProsesseringTjeneste).opprettTaskGruppeForGjenopptaOppdaterFortsett(behandling, false, false);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), behandling);
    }

    @Test
    public void skal_lagre_dokument_og_vurdere_kompletthet_dersom_inntektsmelding_etterlyst() {
        // Arrange - opprette åpen behandling
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_REGISTEROPP);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING, LocalDateTime.now().plusDays(1));

        // Arrange - bygg dok
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", Brevkode.INNTEKTSMELDING);

        // Act
        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(mottattDokument));

        // Assert - sjekk flyt
        verify(behandlingProsesseringTjeneste).opprettTaskGruppeForGjenopptaOppdaterFortsett(behandling, false, false);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), behandling);
    }

    @Test
    public void skal_opprette_revurdering_dersom_inntektsmelding_på_avsluttet_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        Behandling revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getFagsak()).thenReturn(behandling.getFagsak());
        when(revurdering.getAktørId()).thenReturn(behandling.getAktørId());

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", Brevkode.INNTEKTSMELDING);
        when(behandlingsoppretter.opprettNyBehandlingFra(behandling, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)).thenReturn(revurdering);

        // Act
        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(mottattDokument));

        // Assert
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), revurdering);
    }

    @Test
    public void skal_opprette_førstegangsbehandling() {

        Fagsak fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), new Saksnummer("123"), fagsakRepository);
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(123L, "", now(), "123", Brevkode.INNTEKTSMELDING);
        Behandling førstegangsbehandling = mock(Behandling.class);
        when(førstegangsbehandling.getFagsak()).thenReturn(fagsak);
        when(førstegangsbehandling.getAktørId()).thenReturn(AktørId.dummy());
        when(førstegangsbehandling.getFagsakId()).thenReturn(fagsak.getId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty())).thenReturn(førstegangsbehandling);

        // Act
        innhentDokumentTjeneste.mottaDokument(fagsak, List.of(mottattDokument));

        // Assert
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), førstegangsbehandling);
    }

    private Aksjonspunkt opprettAksjonspunkt(Behandling behandling,
                                             AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             LocalDateTime frist) {

        Aksjonspunkt aksjonspunkt = aksjonspunktRepository.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        aksjonspunktRepository.setFrist(aksjonspunkt, frist, Venteårsak.UDEFINERT, null);
        return aksjonspunkt;
    }
}
