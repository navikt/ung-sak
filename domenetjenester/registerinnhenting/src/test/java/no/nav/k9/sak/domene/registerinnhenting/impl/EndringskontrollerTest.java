package no.nav.k9.sak.domene.registerinnhenting.impl;

import static no.nav.k9.sak.behandlingslager.hendelser.StartpunktType.UDEFINERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.registerinnhenting.KontrollerFaktaAksjonspunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class EndringskontrollerTest {

    private KontrollerFaktaAksjonspunktUtleder kontrollerFaktaTjenesteMock;
    private Instance<KontrollerFaktaAksjonspunktUtleder> kontrollerFaktaTjenesterMock;
    private BehandlingskontrollTjeneste behandlingskontrollTjenesteMock;
    private Instance<EndringStartpunktTjeneste> startpunktTjenesteProviderMock;
    private EndringStartpunktTjeneste startpunktTjenesteMock;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjenesteMock;
    private InternalManipulerBehandling internalManipulerBehandling = new InternalManipulerBehandling();

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);

    @BeforeEach
    public void before() {
        kontrollerFaktaTjenesteMock = mock(KontrollerFaktaAksjonspunktUtleder.class);
        when(kontrollerFaktaTjenesteMock.utledAksjonspunkterTilHøyreForStartpunkt(any(), any(StartpunktType.class))).thenReturn(new ArrayList<>());
        kontrollerFaktaTjenesterMock = new UnitTestLookupInstanceImpl<>(kontrollerFaktaTjenesteMock);
        historikkinnslagTjenesteMock = mock(RegisterinnhentingHistorikkinnslagTjeneste.class);

        behandlingskontrollTjenesteMock = mock(BehandlingskontrollTjeneste.class);
        when(behandlingskontrollTjenesteMock.finnAksjonspunktDefinisjonerFraOgMed(any(), any(BehandlingStegType.class), anyBoolean())).thenReturn(new HashSet<>());
        when(behandlingskontrollTjenesteMock.finnBehandlingSteg(any(StartpunktType.class), any(FagsakYtelseType.class), any(BehandlingType.class))).thenReturn(BehandlingStegType.KONTROLLER_FAKTA);

        startpunktTjenesteMock = mock(EndringStartpunktTjeneste.class);
        startpunktTjenesteProviderMock = new UnitTestLookupInstanceImpl<>(startpunktTjenesteMock);
    }

    @Test
    public void skal_håndtere_koarb_utgang_til_inngang() {
        // Arrange
        Behandling revurdering = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, BehandlingStegStatus.UTGANG, BehandlingStegStatus.UTGANG);
        var startpunktKoarb = StartpunktType.KONTROLLER_ARBEIDSFORHOLD;

        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktKoarb);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(0); // Samme steg

        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(UDEFINERT); // Ikke satt
        verify(behandlingskontrollTjenesteMock, times(1)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
        verifyNoInteractions(kontrollerFaktaTjenesteMock);
    }

    @Test
    public void skal_spole_til_start_når_førstegangsbehandling_får_utledet_startpunkt_til_venstre_for_aktivt_steg() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.VURDER_UTTAK, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktBeregning = StartpunktType.BEREGNING;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktBeregning);
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.finnBehandlingSteg(any(StartpunktType.class), any(FagsakYtelseType.class), any(BehandlingType.class))).thenReturn(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(),eq(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING));
    }

    @Test
    public void skal_ikke_spole_til_start_når_førstegangsbehandling_får_utledet_startpunkt_udefinert() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktUdefinert = StartpunktType.UDEFINERT;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUdefinert);

        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        assertThat(behandling.getStartpunkt()).isEqualTo(startpunktUdefinert);
        verify(behandlingskontrollTjenesteMock, times(0)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
    }

    @Test
    public void skal_ikke_oppdatere_startpunkt_ved_tilbakespoling_til_punkt_etter_nåværende_startpunkt() {
        // Arrange
        Behandling revurdering = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);
        var startpunktBeregning = StartpunktType.BEREGNING;
        revurdering.setStartpunkt(startpunktBeregning);

        var startpunktUttak = StartpunktType.UTTAKSVILKÅR;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUttak);

        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(startpunktBeregning);
    }

    @Test
    public void skal_ikke_oppdatere_startpunkt_hvis_vi_ikke_har_grunnlag_for_å_si_at_nytt_startpunkt_er_tidligere() {
        // Arrange
        Behandling revurdering = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);
        StartpunktType startpunktTypePåBehandling = StartpunktType.UDEFINERT;
        revurdering.setStartpunkt(startpunktTypePåBehandling);

        var startpunktUtledetFraEndringssjekk = StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUtledetFraEndringssjekk);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null,
            null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(startpunktTypePåBehandling);
    }

    @Test
    public void skal_spole_til_INNGANG_når_behandlingen_står_i_UTGANG_og_startpunkt_er_samme_steg_som_behandlingen_står_i() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLER_FAKTA, BehandlingStegStatus.UTGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.KONTROLLER_FAKTA;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null,
            historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.finnBehandlingSteg(any(StartpunktType.class), any(FagsakYtelseType.class), any(BehandlingType.class))).thenReturn(BehandlingStegType.KONTROLLER_FAKTA);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(0);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(),eq(BehandlingStegType.KONTROLLER_FAKTA));
    }

    @Test
    public void skal_ikke_spole_til_INNGANG_når_behandlingen_står_i_INNGANG_og_startpunkt_er_samme_steg_som_behandlingen_står_i() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLER_FAKTA, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.KONTROLLER_FAKTA;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null,
            historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(-1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock, times(0)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
    }
}
