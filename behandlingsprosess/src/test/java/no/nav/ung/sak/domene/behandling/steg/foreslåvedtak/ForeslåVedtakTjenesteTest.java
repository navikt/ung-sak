package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.vedtak.impl.KlageVedtakTjeneste;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ForeslåVedtakTjenesteTest {

    public static final String GYLDIG_BREV_TEKST = "<h1>overskrift</h1><p>brødtekst</p>";
    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    @Mock
    private SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekreving;

    @Mock
    private VedtaksbrevTjeneste vedtaksbrevTjeneste;

    @Mock
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    private Behandling behandling;

    private BehandlingskontrollKontekst kontekst;

    private ForeslåVedtakTjeneste tjeneste;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    private ArrayList<Oppgaveinfo> oppgaveinfoerSomReturneres = new ArrayList<>();

    @Mock(strictness = Mock.Strictness.LENIENT)
    private KlageVedtakTjeneste klageVedtakTjeneste;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        when(oppgaveTjeneste.harÅpneOppgaverAvType(any(AktørId.class), any(), any())).thenReturn(false);
        when(sjekkMotTilbakekreving.sjekkMotÅpenIkkeoverlappendeTilbakekreving(any(Behandling.class))).thenReturn(List.of());

        SjekkTilbakekrevingAksjonspunktUtleder sjekkTilbakekrevingAksjonspunktUtleder = Mockito.mock(SjekkTilbakekrevingAksjonspunktUtleder.class);
        when(sjekkTilbakekrevingAksjonspunktUtleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(any())).thenReturn(List.of());
        tjeneste = new ForeslåVedtakTjeneste(behandlingskontrollTjeneste, sjekkTilbakekrevingAksjonspunktUtleder, vedtaksbrevTjeneste, klageVedtakTjeneste, vedtaksbrevValgRepository);
    }

    @Test
    public void oppretterAksjonspunktVedTotrinnskontrollOgSetterStegPåVent() {
        // Arrange
        var aksjonspunkt = leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
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
    public void feilerHvisUgyldigRedigertBrev() {
        VedtaksbrevValgEntitet valgMedTomOverskrift = new VedtaksbrevValgEntitet(
            behandling.getId(), DokumentMalType.ENDRING_INNTEKT, true, false, "<h1></h1>"
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            List.of(valgMedTomOverskrift));

        assertThatThrownBy(() -> tjeneste.foreslåVedtak(behandling, kontekst))
            .isInstanceOf((IllegalStateException.class));
    }

    @Test
    public void okHvisGyldigHtml() {
        VedtaksbrevValgEntitet valgMedGyldigBrev = new VedtaksbrevValgEntitet(
            behandling.getId(), DokumentMalType.ENDRING_INNTEKT, true, false,
            GYLDIG_BREV_TEKST
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            List.of(valgMedGyldigBrev));

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    public void oppretterAksjonspunktBrevRedigering() {
        // Arrange
        behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        when(vedtaksbrevTjeneste.måSkriveBrev(behandling.getId())).thenReturn(
            Set.of(DokumentMalType.MANUELT_VEDTAK_DOK)
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            Collections.emptyList());

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void oppretterAksjonspunktBrevRedigeringHvisIkkeRedigert() {
        // Arrange
        behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        when(vedtaksbrevTjeneste.måSkriveBrev(behandling.getId())).thenReturn(
            Set.of(DokumentMalType.MANUELT_VEDTAK_DOK)
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            List.of(
                new VedtaksbrevValgEntitet(
                    behandling.getId(), DokumentMalType.MANUELT_VEDTAK_DOK, false, false, null)
            )
        );
        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void oppretterAksjonspunktBrevRedigeringHvisAnnenBrevErRedigert() {
        // Arrange
        behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        when(vedtaksbrevTjeneste.måSkriveBrev(behandling.getId())).thenReturn(
            Set.of(DokumentMalType.MANUELT_VEDTAK_DOK)
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            List.of(
                new VedtaksbrevValgEntitet(
                    behandling.getId(), DokumentMalType.ENDRING_INNTEKT, true, false, GYLDIG_BREV_TEKST)
            )
        );
        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void oppretterIkkeAksjonspunktBrevRedigeringHvisBrevErRedigert() {
        // Arrange
        behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        when(vedtaksbrevTjeneste.måSkriveBrev(behandling.getId())).thenReturn(
            Set.of(DokumentMalType.MANUELT_VEDTAK_DOK)
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            List.of(
                new VedtaksbrevValgEntitet(
                    behandling.getId(), DokumentMalType.MANUELT_VEDTAK_DOK, true, false, GYLDIG_BREV_TEKST)
                )
            );

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(0);
    }

    @Test
    public void oppretterIkkeAksjonspunktBrevRedigeringHvisBrevErHindret() {
        // Arrange
        behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        when(vedtaksbrevTjeneste.måSkriveBrev(behandling.getId())).thenReturn(
            Set.of(DokumentMalType.MANUELT_VEDTAK_DOK)
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            List.of(
                new VedtaksbrevValgEntitet(
                    behandling.getId(), DokumentMalType.MANUELT_VEDTAK_DOK, false, true, null)
            )
        );

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(0);
    }

    @Test
    public void oppretterIkkeAksjonspunktBrevRedigeringHvisMåIkkeRedigere() {
        // Arrange
        behandling = TestScenarioBuilder.builderMedSøknad().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        when(vedtaksbrevTjeneste.måSkriveBrev(behandling.getId())).thenReturn(
            Collections.emptySet()
        );
        when(vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())).thenReturn(
            List.of(
                new VedtaksbrevValgEntitet(
                    behandling.getId(), DokumentMalType.MANUELT_VEDTAK_DOK, true, false, GYLDIG_BREV_TEKST)
            )
        );

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(0);
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
    public void utførerUtenAksjonspunktHvisRevurderingOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT).medManueltOpprettet(true))
            .build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(revurdering, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(0);
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
    public void utførerUtenAksjonspunktHvisRevurderingOpprettetManueltOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT).medManueltOpprettet(true))
            .build();
        revurdering.setToTrinnsBehandling();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(revurdering, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(0);
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
    public void skalAvbryteForeslåAksjonspunkterNårDeFinnesPåBehandlingUtenTotrinnskontroll() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);

        // Act
        tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
        assertThat(behandling.getAksjonspunkter()).hasSize(1);
        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK).getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }

    @Test
    public void skalKasteFeilDersomViHarFatteVedtakUtenTotrinnPåBehandling() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FATTER_VEDTAK);

        // Act
        assertThrows(IllegalStateException.class, () -> tjeneste.foreslåVedtak(behandling, kontekst));
    }

    private Aksjonspunkt leggTilAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
    }

}
