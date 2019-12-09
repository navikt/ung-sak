package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.UDEFINERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import javax.enterprise.inject.Instance;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.registerinnhenting.KontrollerFaktaAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@RunWith(CdiRunner.class)
public class EndringskontrollerTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private KontrollerFaktaAksjonspunktUtleder kontrollerFaktaTjenesteMock;
    private Instance<KontrollerFaktaAksjonspunktUtleder> kontrollerFaktaTjenesterMock;
    private BehandlingskontrollTjeneste behandlingskontrollTjenesteMock;
    private Instance<StartpunktTjeneste> startpunktTjenesteProviderMock;
    private StartpunktTjeneste startpunktTjenesteMock;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjenesteMock;
    private InternalManipulerBehandling internalManipulerBehandling = new InternalManipulerBehandling();

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);

    @Before
    public void before() {
        kontrollerFaktaTjenesteMock = mock(KontrollerFaktaAksjonspunktUtleder.class);
        when(kontrollerFaktaTjenesteMock.utledAksjonspunkterTilHøyreForStartpunkt(any(), any(StartpunktType.class))).thenReturn(new ArrayList<>());
        kontrollerFaktaTjenesterMock = new UnitTestLookupInstanceImpl<>(kontrollerFaktaTjenesteMock);
        historikkinnslagTjenesteMock = mock(RegisterinnhentingHistorikkinnslagTjeneste.class);

        behandlingskontrollTjenesteMock = mock(BehandlingskontrollTjeneste.class);
        when(behandlingskontrollTjenesteMock.finnAksjonspunktDefinisjonerFraOgMed(any(), any(BehandlingStegType.class), anyBoolean())).thenReturn(new HashSet<>());

        startpunktTjenesteMock = mock(StartpunktTjeneste.class);
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

        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, repositoryProvider, startpunktTjenesteProviderMock, null, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(UDEFINERT); // Ikke satt
        verify(behandlingskontrollTjenesteMock, times(1)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
        verifyZeroInteractions(kontrollerFaktaTjenesteMock);
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
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, repositoryProvider, startpunktTjenesteProviderMock, null, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(),eq(startpunktBeregning.getBehandlingSteg()));
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

        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, repositoryProvider, startpunktTjenesteProviderMock, null, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

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

        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, repositoryProvider, startpunktTjenesteProviderMock, null, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

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
        internalManipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);
        StartpunktType startpunktTypePåBehandling = StartpunktType.UDEFINERT;
        revurdering.setStartpunkt(startpunktTypePåBehandling);

        var startpunktUtledetFraEndringssjekk = StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUtledetFraEndringssjekk);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, repositoryProvider, startpunktTjenesteProviderMock,
            null, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

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
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, BehandlingStegStatus.UTGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, repositoryProvider, startpunktTjenesteProviderMock,
            null, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(0);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(),eq(BehandlingStegType.SØKERS_RELASJON_TIL_BARN));
    }

    @Test
    public void skal_ikke_spole_til_INNGANG_når_behandlingen_står_i_INNGANG_og_startpunkt_er_samme_steg_som_behandlingen_står_i() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        internalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        Endringskontroller endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, repositoryProvider, startpunktTjenesteProviderMock,
            null, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(-1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L));

        // Assert
        verify(behandlingskontrollTjenesteMock, times(0)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
    }
}
