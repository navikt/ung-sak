package no.nav.ung.sak.domene.registerinnhenting.impl;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.iay.modell.Inntekter;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktTjeneste;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class EndringskontrollerTest {

    private BehandlingskontrollTjeneste behandlingskontrollTjenesteMock;
    private Instance<EndringStartpunktTjeneste> startpunktTjenesteProviderMock;
    private EndringStartpunktTjeneste startpunktTjenesteMock;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjenesteMock;
    private InternalManipulerBehandling internalManipulerBehandling = new InternalManipulerBehandling();

    @BeforeEach
    public void before() {
        historikkinnslagTjenesteMock = mock(RegisterinnhentingHistorikkinnslagTjeneste.class);

        behandlingskontrollTjenesteMock = mock(BehandlingskontrollTjeneste.class);
        when(behandlingskontrollTjenesteMock.finnAksjonspunktDefinisjonerFraOgMed(any(), any(BehandlingStegType.class))).thenReturn(new HashSet<>());
        when(behandlingskontrollTjenesteMock.finnBehandlingSteg(any(StartpunktType.class), any(FagsakYtelseType.class), any(BehandlingType.class))).thenReturn(BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);

        startpunktTjenesteMock = mock(EndringStartpunktTjeneste.class);
        startpunktTjenesteProviderMock = new UnitTestLookupInstanceImpl<>(startpunktTjenesteMock);
    }

    @Test
    public void skal_spole_til_start_når_førstegangsbehandling_får_utledet_startpunkt_til_venstre_for_aktivt_steg() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.VURDER_UTTAK, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktBeregning = StartpunktType.INIT_PERIODER;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktBeregning);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, historikkinnslagTjenesteMock);
        when(behandlingskontrollTjenesteMock.finnBehandlingSteg(any(StartpunktType.class), any(FagsakYtelseType.class), any(BehandlingType.class))).thenReturn(BehandlingStegType.INIT_PERIODER);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntekter.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), eq(BehandlingStegType.INIT_PERIODER));
    }

    @Test
    public void skal_ikke_spole_til_start_når_førstegangsbehandling_får_utledet_startpunkt_udefinert() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktUdefinert = StartpunktType.UDEFINERT;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUdefinert);

        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, null);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntekter.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock, times(0)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
    }

    @Test
    public void skal_spole_til_INNGANG_når_behandlingen_står_i_UTGANG_og_startpunkt_er_samme_steg_som_behandlingen_står_i() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INIT_PERIODER, BehandlingStegStatus.UTGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.INIT_PERIODER;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null,
            historikkinnslagTjenesteMock);
        when(behandlingskontrollTjenesteMock.finnBehandlingSteg(any(StartpunktType.class), any(FagsakYtelseType.class), any(BehandlingType.class))).thenReturn(BehandlingStegType.INIT_PERIODER);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(0);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntekter.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), eq(BehandlingStegType.INIT_PERIODER));
    }

    @Test
    public void skal_ikke_spole_til_INNGANG_når_behandlingen_står_i_INNGANG_og_startpunkt_er_samme_steg_som_behandlingen_står_i() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INIT_PERIODER, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.INIT_PERIODER;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null,
            historikkinnslagTjenesteMock);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(-1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntekter.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock, times(0)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
    }
}
