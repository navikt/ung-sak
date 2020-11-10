package no.nav.k9.sak.domene.risikoklassifisering.tjeneste;

import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.k9.sak.domene.risikoklassifisering.modell.RisikoklassifiseringEntitet;
import no.nav.k9.sak.domene.risikoklassifisering.modell.RisikoklassifiseringRepository;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.rest.FaresignalerRespons;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class RisikovurderingTjenesteTest {

    private final RisikoklassifiseringRepository risikoklassifiseringRepository = mock(RisikoklassifiseringRepository.class);

    private final HentFaresignalerTjeneste hentFaresignalerTjeneste = mock(HentFaresignalerTjeneste.class);

    private final KontrollresultatMapper mapper = mock(KontrollresultatMapper.class);

    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);

    private RisikovurderingTjeneste risikovurderingTjeneste;

    private Behandling behandling;


    @BeforeEach
    public void setup() {
        var scenarioFørstegang = TestScenarioBuilder.builderMedSøknad();
        behandling = scenarioFørstegang.lagMocked();
        risikovurderingTjeneste = new RisikovurderingTjeneste(risikoklassifiseringRepository,
            behandlingRepository,
            hentFaresignalerTjeneste,
            mapper, behandlingskontrollTjeneste);
    }

    @Test
    public void skal_teste_at_risikowrapper_lagres_for_en_behandling_som_matcher_uuid() {
        // Arrange
        UUID uuid = behandling.getUuid();
        when(behandlingRepository.hentBehandlingHvisFinnes(uuid)).thenReturn(Optional.of(behandling));
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.empty());

        // Act
        risikovurderingTjeneste.lagreKontrollresultat(lagWrapper(uuid, Kontrollresultat.HØY));

        // Assert
        verify(risikoklassifiseringRepository).lagreRisikoklassifisering(any(), anyLong());
    }

    @Test
    public void skal_teste_at_risikowrapper_ikke_lagres_for_en_behandling_når_det_allerede_finnes_et_resultat() {
        // Arrange
        UUID uuid = behandling.getUuid();
        when(behandlingRepository.hentBehandlingHvisFinnes(uuid)).thenReturn(Optional.of(behandling));
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(RisikoklassifiseringEntitet.builder().buildFor(123L)));

        // Act
        risikovurderingTjeneste.lagreKontrollresultat(lagWrapper(uuid, Kontrollresultat.HØY));

        // Assert
        verify(risikoklassifiseringRepository, times(0)).lagreRisikoklassifisering(any(), anyLong());
    }


    @Test
    public void skal_teste_at_risikowrapper_ikke_lagres_når_det_ikke_finnes_behandling_med_matchende_uuid() {
        // Arrange
        UUID uuid = behandling.getUuid();
        when(behandlingRepository.hentBehandlingHvisFinnes(uuid)).thenReturn(Optional.empty());

        // Act
        risikovurderingTjeneste.lagreKontrollresultat(lagWrapper(uuid, Kontrollresultat.HØY));

        // Assert
        verifyNoInteractions(risikoklassifiseringRepository);
    }

    @Test
    public void skal_teste_at_vi_returnerer_tom_hvis_ikke_noe_resultat_er_lagret() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.empty());

        // Act
        Optional<FaresignalWrapper> faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(faresignalWrapper).isNotPresent();
        verifyNoInteractions(hentFaresignalerTjeneste);
    }

    @Test
    public void skal_teste_at_vi_ikke_henter_resultat_fra_fprisk_ved_ikke_høy_risiko() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.IKKE_HØY)));

        // Act
        Optional<FaresignalWrapper> faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(faresignalWrapper).isPresent();
        assertThat(faresignalWrapper.get().getKontrollresultat()).isEqualTo(Kontrollresultat.IKKE_HØY);
        assertThat(faresignalWrapper.get().getMedlFaresignaler()).isNull();
        assertThat(faresignalWrapper.get().getIayFaresignaler()).isNull();
        verifyNoInteractions(mapper);
        verifyNoInteractions(hentFaresignalerTjeneste);
    }

    @Test
    public void skal_teste_at_vi_henter_resultat_fra_fprisk_ved_høy_risiko() {
        // Arrange
        UUID uuid = behandling.getUuid();
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.HØY)));
        FaresignalerRespons respons = new FaresignalerRespons();
        when(hentFaresignalerTjeneste.hentFaresignalerForBehandling(uuid)).thenReturn(Optional.of(respons));
        FaresignalWrapper wrapper = new FaresignalWrapper();
        when(mapper.fraFaresignalRespons(any())).thenReturn(wrapper);

        // Act
        Optional<FaresignalWrapper> faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(faresignalWrapper).isPresent();
        verify(hentFaresignalerTjeneste).hentFaresignalerForBehandling(uuid);
        verify(mapper).fraFaresignalRespons(respons);
    }

    @Test
    public void skal_teste_at_aksjonspunkt_opprettes_når_risiko_er_høy() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.HØY)));

        // Act
        boolean skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(behandling.getId());

        // Assert
        assertThat(skalOppretteAksjonspunkt).isTrue();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_når_risiko_er_lav() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.IKKE_HØY)));

        // Act
        boolean skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(behandling.getId());

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_det_mangler_kontrollresultat() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.empty());

        // Act
        boolean skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(behandling.getId());

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    private RisikoklassifiseringEntitet lagEntitet(Kontrollresultat kontrollresultat) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).buildFor(123L);
    }

    private KontrollresultatWrapper lagWrapper(UUID uuid, Kontrollresultat resultat) {
        return new KontrollresultatWrapper(uuid, resultat);
    }
}
