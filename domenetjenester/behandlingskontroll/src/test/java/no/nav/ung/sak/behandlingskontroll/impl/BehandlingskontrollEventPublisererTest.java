package no.nav.ung.sak.behandlingskontroll.impl;

import static no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingskontroll.testutilities.TestScenario;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.db.util.CdiDbAwareTest;

@CdiDbAwareTest
public class BehandlingskontrollEventPublisererTest {
    private final BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private final FagsakYtelseType fagsakYtelseType = FagsakYtelseType.FORELDREPENGER;

    private static final BehandlingStegType STEG_1 = BehandlingStegType.INNHENT_REGISTEROPP;
    private static final BehandlingStegType STEG_2 = BehandlingStegType.KONTROLLER_FAKTA;
    private static final BehandlingStegType STEG_3 = BehandlingStegType.VURDER_KOMPLETTHET;

    private static final BehandlingStegType STEG_4 = BehandlingStegType.VURDER_TILBAKETREKK;

    @Inject
    BehandlingskontrollEventPubliserer eventPubliserer;

    @Inject
    BehandlingskontrollServiceProvider serviceProvider;

    // No Inject
    BehandlingskontrollTjenesteImpl kontrollTjeneste;

    @BeforeEach
    public void setup() {
        BehandlingModellImpl behandlingModell = byggModell();

        kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider) {
            @Override
            protected BehandlingModellImpl getModell(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
                return behandlingModell;
            }
        };

        TestEventObserver.startCapture();
    }

    @AfterEach
    public void after() {
        TestEventObserver.reset();
    }

    @Test
    public void skal_fyre_event_for_aksjonspunkt_funnet_ved_prosessering() throws Exception {
        TestScenario scenario = TestScenario.dummyScenario();
        Behandling behandling = scenario.lagre(serviceProvider);

        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling.getId());

        BehandlingStegType stegType = BehandlingStegType.VURDER_KOMPLETTHET;

        Aksjonspunkt aksjonspunkt = serviceProvider.getAksjonspunktKontrollRepository().leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, stegType);
        kontrollTjeneste.aksjonspunkterEndretStatus(kontekst, stegType, List.of(aksjonspunkt));

        AksjonspunktDefinisjon[] ads = { AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT };
        TestEventObserver.containsExactly(ads);
    }

    @Test
    public void skal_fyre_event_for_behandlingskontroll_startet_stoppet_ved_prosessering() throws Exception {
        // Arrange
        TestScenario scenario = nyttScenario(STEG_1);

        Behandling behandling = scenario.lagre(serviceProvider);

        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling.getId());

        // Act
        kontrollTjeneste.prosesserBehandling(kontekst);

        // Assert

        BehandlingskontrollEvent startEvent = new BehandlingskontrollEvent.StartetEvent(null, null, STEG_1, null);
        BehandlingskontrollEvent stoppEvent = new BehandlingskontrollEvent.StoppetEvent(null, null, STEG_4, BehandlingStegStatus.UTGANG);
        TestEventObserver.containsExactly(startEvent, stoppEvent);

    }

    @Test
    public void skal_fyre_event_for_behandlingskontroll_behandlingsteg_status_endring_ved_prosessering() throws Exception {
        // Arrange
        TestScenario scenario = nyttScenario(STEG_1);

        Behandling behandling = scenario.lagre(serviceProvider);

        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling.getId());

        // Act
        kontrollTjeneste.prosesserBehandling(kontekst);

        // Assert

        BehandlingStegStatusEvent steg1StatusEvent0 = new BehandlingStegStatusEvent(kontekst, STEG_1, null,
            BehandlingStegStatus.STARTET);
        BehandlingStegStatusEvent steg1StatusEvent1 = new BehandlingStegStatusEvent(kontekst, STEG_1, BehandlingStegStatus.STARTET,
            BehandlingStegStatus.UTFØRT);
        BehandlingStegStatusEvent steg2StatusEvent0 = new BehandlingStegStatusEvent(kontekst, STEG_2, null,
            BehandlingStegStatus.STARTET);
        BehandlingStegStatusEvent steg2StatusEvent = new BehandlingStegStatusEvent(kontekst, STEG_2, BehandlingStegStatus.STARTET,
            BehandlingStegStatus.UTFØRT);
        BehandlingStegStatusEvent steg3StatusEvent0 = new BehandlingStegStatusEvent(kontekst, STEG_2, null,
            BehandlingStegStatus.STARTET);
        BehandlingStegStatusEvent steg3StatusEvent = new BehandlingStegStatusEvent(kontekst, STEG_3, BehandlingStegStatus.STARTET,
            BehandlingStegStatus.UTFØRT);
        BehandlingStegStatusEvent steg4StatusEvent0 = new BehandlingStegStatusEvent(kontekst, STEG_4, null,
            BehandlingStegStatus.STARTET);
        BehandlingStegStatusEvent steg4StatusEvent1 = new BehandlingStegStatusEvent(kontekst, STEG_4, BehandlingStegStatus.STARTET,
            BehandlingStegStatus.UTGANG);
        TestEventObserver.containsExactly(steg1StatusEvent0, steg1StatusEvent1 //
            , steg2StatusEvent0, steg2StatusEvent//
            , steg3StatusEvent0, steg3StatusEvent//
            , steg4StatusEvent0
            , steg4StatusEvent1//
        );
    }

    @Test
    public void skal_fyre_event_for_behandlingskontroll_tilbakeføring_ved_prosessering() throws Exception {
        // Arrange
        TestScenario scenario = nyttScenario(STEG_3);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_TILBAKETREKK, STEG_4);

        Behandling behandling = scenario.lagre(serviceProvider);

        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling.getId());

        // Act
        kontrollTjeneste.prosesserBehandling(kontekst);

        // Assert
        // TODO (essv): Vanskelig å overstyre SUT til å gjøre tilbakehopp i riktig retning, her gjøres det fremover.
        // Den trenger et åpent aksjonspunkt som ligger før startsteget
        BehandlingStegOvergangEvent tilbakeføring3_4 = nyOvergangEvent(kontekst, STEG_3, BehandlingStegStatus.UTFØRT, STEG_4, null);
        TestEventObserver.containsExactly(tilbakeføring3_4);
    }

    @Test
    public void skal_fyre_event_for_behandlingskontroll_behandlingsteg_overgang_ved_prosessering() throws Exception {
        // Arrange
        TestScenario scenario = nyttScenario(STEG_1);

        Behandling behandling = scenario.lagre(serviceProvider);

        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling.getId());

        // Act
        kontrollTjeneste.prosesserBehandling(kontekst);

        // Assert

        BehandlingStegOvergangEvent overgang1_2 = nyOvergangEvent(kontekst, STEG_1, BehandlingStegStatus.UTFØRT, STEG_2, null);
        BehandlingStegOvergangEvent overgang2_3 = nyOvergangEvent(kontekst, STEG_2, BehandlingStegStatus.UTFØRT, STEG_3, null);
        BehandlingStegOvergangEvent overgang3_4 = nyOvergangEvent(kontekst, STEG_3, BehandlingStegStatus.UTFØRT, STEG_4, null);
        TestEventObserver.containsExactly(overgang1_2, overgang2_3, overgang3_4);
    }

    protected TestScenario nyttScenario(BehandlingStegType startSteg) {
        TestScenario scenario = TestScenario.dummyScenario();
        scenario.medBehandlingStegStart(startSteg);
        return scenario;
    }

    private BehandlingStegOvergangEvent nyOvergangEvent(BehandlingskontrollKontekst kontekst,
                                                        BehandlingStegType steg1, BehandlingStegStatus steg1Status, BehandlingStegType steg2, BehandlingStegStatus steg2Status) {
        return new BehandlingStegOvergangEvent(kontekst, lagTilstand(steg1, steg1Status),
            lagTilstand(steg2, steg2Status));
    }

    private BehandlingStegTilstandSnapshot lagTilstand(BehandlingStegType stegType,
                                                       BehandlingStegStatus stegStatus) {
        return new BehandlingStegTilstandSnapshot(1L, stegType, stegStatus);
    }

    private BehandlingModellImpl byggModell() {
        // Arrange - noen utvalge, tilfeldige aksjonspunkter
        AksjonspunktDefinisjon a0_1 = AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST;
        AksjonspunktDefinisjon a1_1 = AksjonspunktDefinisjon.VURDER_FEILUTBETALING;
        AksjonspunktDefinisjon a2_1 = AksjonspunktDefinisjon.VURDER_TILBAKETREKK;

        DummySteg steg = new DummySteg();
        DummySteg steg0 = new DummySteg(opprettForAksjonspunkt(a2_1));
        DummySteg steg1 = new DummySteg();
        DummySteg steg2 = new DummySteg();

        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, steg, ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, steg0, ap(a0_1)),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, steg1, ap(a1_1)),
            new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, steg2, ap(a2_1)));

        return ModifiserbarBehandlingModell.setupModell(behandlingType, fagsakYtelseType, modellData);
    }

    private List<AksjonspunktDefinisjon> ap(AksjonspunktDefinisjon... aksjonspunktDefinisjoner) {
        return List.of(aksjonspunktDefinisjoner);
    }
}
