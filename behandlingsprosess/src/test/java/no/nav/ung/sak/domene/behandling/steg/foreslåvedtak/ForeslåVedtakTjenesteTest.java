package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.FormidlingTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.Oppgaveinfo;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.økonomi.tilbakekreving.samkjøring.SjekkTilbakekrevingAksjonspunktUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ForeslåVedtakTjenesteTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Inject
    private @Any Instance<ForeslåVedtakManueltUtleder> foreslåVedtakManueltUtledere;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    @Mock
    private SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekreving;

    @Mock
    private FormidlingTjeneste formidlingTjeneste;

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
        when(sjekkMotTilbakekreving.sjekkMotÅpenIkkeoverlappendeTilbakekreving(any(Behandling.class))).thenReturn(List.of());

        SjekkTilbakekrevingAksjonspunktUtleder sjekkTilbakekrevingAksjonspunktUtleder = Mockito.mock(SjekkTilbakekrevingAksjonspunktUtleder.class);
        when(sjekkTilbakekrevingAksjonspunktUtleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(any())).thenReturn(List.of());
        tjeneste = new ForeslåVedtakTjeneste(behandlingskontrollTjeneste, sjekkTilbakekrevingAksjonspunktUtleder, foreslåVedtakManueltUtledere, formidlingTjeneste);
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
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET);

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
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_INNTEKT);

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
