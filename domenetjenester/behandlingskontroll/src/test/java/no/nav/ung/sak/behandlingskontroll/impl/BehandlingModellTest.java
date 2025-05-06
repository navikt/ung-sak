package no.nav.ung.sak.behandlingskontroll.impl;

import static no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegUtfall;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.StegProsesseringResultat;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingskontroll.testutilities.TestScenario;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.db.util.CdiDbAwareTest;

@SuppressWarnings("resource")
@CdiDbAwareTest
public class BehandlingModellTest {

    private static final LocalDateTime FRIST_TID = LocalDateTime.now().plusWeeks(4).withNano(0);

    private final BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private final FagsakYtelseType fagsakYtelseType = FagsakYtelseType.SVANGERSKAPSPENGER;

    private static final BehandlingStegType STEG_1 = BehandlingStegType.INNHENT_REGISTEROPP;
    private static final BehandlingStegType STEG_2 = BehandlingStegType.KONTROLLER_FAKTA;
    private static final BehandlingStegType STEG_3 = BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
    private static final BehandlingStegType STEG_4 = BehandlingStegType.VURDER_OPPTJENING_FAKTA;

    @Inject
    private BehandlingskontrollTjeneste kontrollTjeneste;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    private final DummySteg nullSteg = new DummySteg();
    private final DummyVenterSteg nullVenterSteg = new DummyVenterSteg();
    private final DummySteg aksjonspunktSteg = new DummySteg(opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
    private final DummySteg aksjonspunktModifisererSteg = new DummySteg(opprettForAksjonspunktMedFrist(
        AksjonspunktDefinisjon.KONTROLLER_INNTEKT, Venteårsak.AVV_DOK, FRIST_TID));

    @Test
    public void skal_finne_aksjonspunkter_som_ligger_etter_et_gitt_steg() {
        // Arrange - noen utvalge, tilfeldige aksjonspunkter
        AksjonspunktDefinisjon a0_1 = AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST;
        AksjonspunktDefinisjon a1_1 = AksjonspunktDefinisjon.KONTROLLER_INNTEKT;
        AksjonspunktDefinisjon a2_1 = AksjonspunktDefinisjon.VURDER_FEILUTBETALING;

        DummySteg steg = new DummySteg();
        DummySteg steg0 = new DummySteg();
        DummySteg steg1 = new DummySteg();
        DummySteg steg2 = new DummySteg();

        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, steg, ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, steg0, ap(a0_1)),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, steg1, ap(a1_1)),
            new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, steg2, ap(a2_1)));

        BehandlingModellImpl modell = setupModell(modellData);

        Set<String> ads;

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_1);

        assertThat(ads).containsOnly(a0_1.getKode(), a1_1.getKode(), a2_1.getKode());

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_2);

        assertThat(ads).containsOnly(a1_1.getKode(), a2_1.getKode());

        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_3);

        assertThat(ads).containsOnly(a2_1.getKode());
        ads = modell.finnAksjonspunktDefinisjonerEtter(STEG_4);

        assertThat(ads).

            isEmpty();

    }

    @Test
    public void skal_stoppe_på_steg_3_når_får_aksjonspunkt() throws Exception {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, aksjonspunktSteg, ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(AksjonspunktDefinisjon.KONTROLLER_INNTEKT)),
            new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, nullSteg, ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.dummyScenario();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);
        BehandlingStegUtfall siste = modell.prosesserFra(STEG_1, visitor);

        assertThat(siste.getBehandlingStegType()).isEqualTo(STEG_3);
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2, STEG_3));
    }

    public List<AksjonspunktDefinisjon> ap(AksjonspunktDefinisjon... aksjonspunktDefinisjoner) {
        return List.of(aksjonspunktDefinisjoner);
    }

    @Test
    public void skal_kjøre_til_siste_når_ingen_gir_aksjonspunkt() throws Exception {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.dummyScenario();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);
        BehandlingStegUtfall siste = modell.prosesserFra(STEG_1, visitor);

        assertThat(siste).isNull();
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2, STEG_3));
    }

    @Test
    public void skal_stoppe_når_settes_på_vent_deretter_fortsette() {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullVenterSteg, ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.dummyScenario();
        Behandling behandling = scenario.lagre(serviceProvider);

        // Act 1
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);

        BehandlingStegUtfall første = modell.prosesserFra(STEG_1, visitor);

        assertThat(første).isNotNull();
        assertThat(første.getBehandlingStegType()).isEqualTo(STEG_2);
        assertThat(første.getResultat()).isEqualTo(BehandlingStegStatus.STARTET);
        assertThat(visitor.kjørteSteg).isEqualTo(List.of(STEG_1, STEG_2));

        // Act 2
        BehandlingStegVisitorUtenLagring visitorNeste = lagVisitor(behandling);
        BehandlingStegUtfall neste = modell.prosesserFra(STEG_2, visitorNeste);

        assertThat(neste).isNotNull();
        assertThat(neste.getBehandlingStegType()).isEqualTo(STEG_2);
        assertThat(neste.getResultat()).isEqualTo(BehandlingStegStatus.VENTER);
        assertThat(visitorNeste.kjørteSteg).isEqualTo(List.of(STEG_2));

        // Act 3
        BehandlingStegVisitorUtenLagring visitorNeste2 = lagVisitor(behandling);
        BehandlingStegUtfall neste2 = modell.prosesserFra(STEG_2, visitorNeste2);

        assertThat(neste2).isNotNull();
        assertThat(neste2.getBehandlingStegType()).isEqualTo(STEG_2);
        assertThat(neste2.getResultat()).isEqualTo(BehandlingStegStatus.VENTER);
        assertThat(visitorNeste2.kjørteSteg).isEqualTo(List.of(STEG_2));

        // Act 4
        BehandlingStegVisitorVenterUtenLagring gjenoppta = lagVisitorVenter(behandling);

        BehandlingStegUtfall fortsett = modell.prosesserFra(STEG_2, gjenoppta);
        assertThat(fortsett).isNull();
        assertThat(gjenoppta.kjørteSteg).isEqualTo(List.of(STEG_2, STEG_3));
    }

    @Test
    public void skal_feile_ved_gjenopptak_vanlig_steg() {

        Assertions.assertThrows(IllegalStateException.class, () -> {
            // Arrange
            List<TestStegKonfig> modellData = List.of(
                new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap()),
                new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap()),
                new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap()));
            BehandlingModellImpl modell = setupModell(modellData);

            TestScenario scenario = TestScenario.dummyScenario();
            Behandling behandling = scenario.lagre(serviceProvider);

            // Act 1
            BehandlingStegVisitorVenterUtenLagring visitor = lagVisitorVenter(behandling);
            modell.prosesserFra(STEG_1, visitor);
        });
    }

    @Test
    public void tilbakefører_til_tidligste_steg_med_åpent_aksjonspunkt() {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = STEG_2.getAksjonspunktDefinisjonerUtgang().get(0);
        DummySteg tilbakeføringssteg = new DummySteg(true, opprettForAksjonspunkt(aksjonspunktDefinisjon));
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, nullSteg, ap(aksjonspunktDefinisjon)),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, tilbakeføringssteg, ap()),
            new TestStegKonfig(STEG_4, behandlingType, fagsakYtelseType, nullSteg, ap()));
        BehandlingModellImpl modell = setupModell(modellData);

        TestScenario scenario = TestScenario.dummyScenario();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);

        Aksjonspunkt aksjonspunkt = serviceProvider.getAksjonspunktKontrollRepository().leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon,
            STEG_1);
        serviceProvider.getAksjonspunktKontrollRepository().setReåpnet(aksjonspunkt);

        BehandlingStegUtfall siste = modell.prosesserFra(STEG_3, visitor);
        assertThat(siste.getBehandlingStegType()).isEqualTo(STEG_3);

        Behandling beh = hentBehandling(behandling.getId());
        assertThat(beh.getAktivtBehandlingSteg()).isEqualTo(STEG_2);
    }


    @Test
    public void skal_modifisere_aksjonspunktet_ved_å_kalle_funksjon_som_legger_til_frist() throws Exception {
        // Arrange
        List<TestStegKonfig> modellData = List.of(
            new TestStegKonfig(STEG_1, behandlingType, fagsakYtelseType, aksjonspunktModifisererSteg, ap()),
            new TestStegKonfig(STEG_2, behandlingType, fagsakYtelseType, nullSteg, ap()),
            new TestStegKonfig(STEG_3, behandlingType, fagsakYtelseType, nullSteg, ap(AksjonspunktDefinisjon.VURDER_FEILUTBETALING)));
        BehandlingModellImpl modell = setupModell(modellData);
        TestScenario scenario = TestScenario.dummyScenario();
        Behandling behandling = scenario.lagre(serviceProvider);
        BehandlingStegVisitorUtenLagring visitor = lagVisitor(behandling);

        // Act
        modell.prosesserFra(STEG_1, visitor);

        // Assert
        Behandling beh = hentBehandling(behandling.getId());
        assertThat(beh.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(beh.getÅpneAksjonspunkter().get(0).getFristTid()).isEqualTo(FRIST_TID);
    }

    private Behandling hentBehandling(Long behandlingId) {
        return serviceProvider.hentBehandling(behandlingId);
    }

    private BehandlingModellImpl setupModell(List<TestStegKonfig> resolve) {
        return ModifiserbarBehandlingModell.setupModell(behandlingType, fagsakYtelseType, resolve);
    }

    private BehandlingStegVisitorUtenLagring lagVisitor(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling);
        var lokalServiceProvider = new BehandlingskontrollServiceProvider(serviceProvider.getEntityManager(), serviceProvider.getBehandlingModellRepository(),
            null);
        return new BehandlingStegVisitorUtenLagring(lokalServiceProvider, kontekst);
    }

    private BehandlingStegVisitorVenterUtenLagring lagVisitorVenter(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = kontrollTjeneste.initBehandlingskontroll(behandling);
        var lokalServiceProvider = new BehandlingskontrollServiceProvider(serviceProvider.getEntityManager(), serviceProvider.getBehandlingModellRepository(),
            null);
        return new BehandlingStegVisitorVenterUtenLagring(lokalServiceProvider, kontekst);
    }

    static class BehandlingStegVisitorUtenLagring extends TekniskBehandlingStegVisitor {
        List<BehandlingStegType> kjørteSteg = new ArrayList<>();

        BehandlingStegVisitorUtenLagring(BehandlingskontrollServiceProvider repositoryProvider,
                                         BehandlingskontrollKontekst kontekst) {
            super(repositoryProvider, kontekst);
        }

        @Override
        protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling, BehandlingStegVisitor stegVisitor) {
            // bypass savepoint
            this.kjørteSteg.add(stegVisitor.getStegModell().getBehandlingStegType());
            return super.prosesserSteg(stegVisitor);
        }
    }

    static class BehandlingStegVisitorVenterUtenLagring extends TekniskBehandlingStegVenterVisitor {
        List<BehandlingStegType> kjørteSteg = new ArrayList<>();

        BehandlingStegVisitorVenterUtenLagring(BehandlingskontrollServiceProvider repositoryProvider,
                                               BehandlingskontrollKontekst kontekst) {
            super(repositoryProvider, kontekst);
        }

        @Override
        protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling, BehandlingStegVisitor stegVisitor) {
            // bypass savepoint
            this.kjørteSteg.add(stegVisitor.getStegModell().getBehandlingStegType());
            return super.prosesserSteg(stegVisitor);
        }
    }
}
