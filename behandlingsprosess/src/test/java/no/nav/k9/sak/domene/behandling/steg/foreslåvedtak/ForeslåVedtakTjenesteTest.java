package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.dokument.bestill.DokumentBehandlingTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.Oppgaveinfo;
import no.nav.k9.sak.test.util.Whitebox;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ForeslåVedtakTjenesteTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    @Spy
    private HistorikkRepository historikkRepository;

    private Behandling behandling;

    private BehandlingskontrollKontekst kontekst;

    private ForeslåVedtakTjeneste tjeneste;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    private ArrayList<Oppgaveinfo> oppgaveinfoerSomReturneres = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        historikkRepository = repositoryProvider.getHistorikkRepository();

        behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        when(oppgaveTjeneste.harÅpneOppgaverAvType(any(AktørId.class), any(), any())).thenReturn(false);

        SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste = new SjekkMotEksisterendeOppgaverTjeneste(historikkRepository, oppgaveTjeneste);
        tjeneste = new ForeslåVedtakTjeneste(fagsakRepository, behandlingskontrollTjeneste, false, sjekkMotEksisterendeOppgaverTjeneste);
    }

    @Test
    public void oppretterAksjonspunktVedTotrinnskontrollOgSetterStegPåVent() {
        // Arrange
        var aksjonspunkt = leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT);
        Whitebox.setInternalState(aksjonspunkt, "status", AksjonspunktStatus.UTFØRT);
        Whitebox.setInternalState(aksjonspunkt, "toTrinnsBehandling", true);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }

    @Test
    public void setterTotrinnskontrollPaBehandlingHvisIkkeSattFraFør() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET);

        // Act
        tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isTrue();
    }

    @Test
    public void setterPåVentHvisÅpentAksjonspunktVedtakUtenTotrinnskontroll() {
        // Arrange
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void nullstillerBehandlingHvisDetEksistererVedtakUtenTotrinnAksjonspunkt() {
        // Arrange
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);

        // Act
        tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    public void setterStegTilUtførtUtenAksjonspunktDersomIkkeTotorinnskontroll() {
        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void setterIkkeTotrinnskontrollPaBehandlingHvisDetIkkeErTotrinnskontroll() {
        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    public void lagerRiktigAksjonspunkterNårDetErOppgaveriGsak() {
        // Arrange
        oppgaveinfoerSomReturneres.add(Oppgaveinfo.VURDER_KONST_YTELSE);
        oppgaveinfoerSomReturneres.add(Oppgaveinfo.VURDER_DOKUMENT);

        // Act
        @SuppressWarnings("unused")
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        //TODO: Skrives inn når VKY for K9 er på plass
        // Assert
        //assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        //verify(historikkRepository, times(2)).lagre(any());
        //assertThat(stegResultat.getAksjonspunktListe().contains(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK)).isTrue();
        //assertThat(stegResultat.getAksjonspunktListe().contains(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK)).isTrue();
    }

    @Test
    public void lagerIkkeNyeAksjonspunkterNårAksjonspunkterAlleredeFinnes() {
        /*
        // Arrange
        var aksjonspunkt = leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK);
        Whitebox.setInternalState(aksjonspunkt, "status", AksjonspunktStatus.UTFØRT);

        oppgaveinfoerSomReturneres.add(Oppgaveinfo.VURDER_KONST_YTELSE);
        oppgaveinfoerSomReturneres.add(Oppgaveinfo.VURDER_DOKUMENT);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(historikkRepository, times(0)).lagre(any());
         */
    }

    @Test
    public void utførerUtenAksjonspunktHvisRevurderingIkkeOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING).medManueltOpprettet(true))
            .build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(revurdering, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void utførerUtenAksjonspunktHvisRevurderingIkkeManueltOpprettetOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        behandling.setToTrinnsBehandling();

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING).medManueltOpprettet(true))
            .build();
        revurdering.setToTrinnsBehandling();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(revurdering, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void oppretterAksjonspunktVedTotrinnskontrollForRevurdering() {
        // Arrange
        behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }

    @Test
    public void skalAvbryteForeslåOgFatteVedtakAksjonspunkterNårDeFinnesPåBehandlingUtenTotrinnskontroll() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FATTER_VEDTAK);

        // Act
        tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
        assertThat(behandling.getAksjonspunkter()).hasSize(2);
        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK).getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FATTER_VEDTAK).getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }

    private Aksjonspunkt leggTilAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
    }

}
