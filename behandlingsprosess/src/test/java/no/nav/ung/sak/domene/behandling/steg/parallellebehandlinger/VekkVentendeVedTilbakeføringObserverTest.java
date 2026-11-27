package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FATTE_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FORESLÅ_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INNHENT_REGISTEROPP;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.IVERKSETT_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.SIMULER_OPPDRAG;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.START_STEG;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_VEDTAKSPRIORITET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VekkVentendeVedTilbakeføringObserverTest {

    /** Modell for førstegangsbehandling og revurdering når bryteren er av: ventepunktet for vedtaksprioritet mangler. */
    private static final List<BehandlingStegType> MODELL_UTEN_VENTEPUNKT_FOR_VEDTAKSPRIORITET = List.of(START_STEG, INNHENT_REGISTEROPP, VURDER_KOMPLETTHET, KONTROLLER_REGISTER_INNTEKT,
        FORESLÅ_BEHANDLINGSRESULTAT, BEREGN_YTELSE, SIMULER_OPPDRAG, FORESLÅ_VEDTAK, FATTE_VEDTAK, IVERKSETT_VEDTAK);

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private Behandling revurdering;
    private Behandling kontroll;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);

        var førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        førstegangsbehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        førstegangsbehandling.avsluttBehandling();
        lagreOgFlush(førstegangsbehandling);

        revurdering = nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, SIMULER_OPPDRAG);
        kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);
    }

    @Test
    void vekker_ventende_behandling_når_behandlingen_med_vedtaksprioritet_hopper_tilbake_til_før_ventepunktet() {
        settPåVentIVentepunktForVedtaksprioritet(kontroll);

        observer(VurderVedtaksprioritetStegTest.MODELL_MED_VENTEPUNKT_FOR_VEDTAKSPRIORITET, true).vedTilbakeføring(tilbakeføring(revurdering, BEREGN_YTELSE, VURDER_KOMPLETTHET));

        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(kontroll, false);
    }

    @Test
    void vekker_ventende_behandling_når_behandlingen_med_vedtaksprioritet_hopper_tilbake_til_ventepunktet() {
        settPåVentIVentepunktForVedtaksprioritet(kontroll);

        observer(VurderVedtaksprioritetStegTest.MODELL_MED_VENTEPUNKT_FOR_VEDTAKSPRIORITET, true).vedTilbakeføring(tilbakeføring(revurdering, FATTE_VEDTAK, VURDER_VEDTAKSPRIORITET));

        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(kontroll, false);
    }

    @Test
    void vekker_ikke_når_behandlingen_med_vedtaksprioritet_hopper_tilbake_til_et_steg_etter_ventepunktet() {
        settPåVentIVentepunktForVedtaksprioritet(kontroll);

        observer(VurderVedtaksprioritetStegTest.MODELL_MED_VENTEPUNKT_FOR_VEDTAKSPRIORITET, true).vedTilbakeføring(tilbakeføring(revurdering, FATTE_VEDTAK, FORESLÅ_VEDTAK));

        verifiserIngenVekking();
    }

    @Test
    void vekker_ikke_når_behandlingen_hopper_tilbake_fra_ventepunktet_for_vedtaksprioritet() {
        settPåVentIVentepunktForVedtaksprioritet(kontroll);

        observer(VurderVedtaksprioritetStegTest.MODELL_MED_VENTEPUNKT_FOR_VEDTAKSPRIORITET, true).vedTilbakeføring(tilbakeføring(revurdering, VURDER_VEDTAKSPRIORITET, VURDER_KOMPLETTHET));

        verifiserIngenVekking();
    }

    @Test
    void vekker_ikke_behandling_som_ikke_venter_i_ventepunktet_for_vedtaksprioritet() {
        observer(VurderVedtaksprioritetStegTest.MODELL_MED_VENTEPUNKT_FOR_VEDTAKSPRIORITET, true).vedTilbakeføring(tilbakeføring(revurdering, BEREGN_YTELSE, VURDER_KOMPLETTHET));

        verifiserIngenVekking();
    }

    @Test
    void gjør_ingenting_når_bryteren_er_av() {
        settPåVentIVentepunktForVedtaksprioritet(kontroll);

        observer(MODELL_UTEN_VENTEPUNKT_FOR_VEDTAKSPRIORITET, false).vedTilbakeføring(tilbakeføring(revurdering, BEREGN_YTELSE, VURDER_KOMPLETTHET));

        verifiserIngenVekking();
    }

    @Test
    void sammenligner_aldri_mot_ventepunktet_for_vedtaksprioritet_i_modellen() {
        settPåVentIVentepunktForVedtaksprioritet(kontroll);

        // Modellen uten ventepunktet for vedtaksprioritet kaster exception ved oppslag på ventepunktet, som BehandlingModellImpl.indexOf.
        observer(MODELL_UTEN_VENTEPUNKT_FOR_VEDTAKSPRIORITET, true).vedTilbakeføring(tilbakeføring(revurdering, BEREGN_YTELSE, VURDER_KOMPLETTHET));

        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(kontroll, false);
    }

    private VekkVentendeVedTilbakeføringObserver observer(List<BehandlingStegType> modell, boolean bryterPå) {
        return new VekkVentendeVedTilbakeføringObserver(behandlingRepository, new ParallelleBehandlingerRepository(entityManager),
            VurderVedtaksprioritetStegTest.behandlingskontrollMedModell(modell), behandlingProsesseringTjeneste,
            new KontrollbehandlingBryter(bryterPå, bryterPå));
    }

    private BehandlingStegTilbakeføringEvent tilbakeføring(Behandling behandling, BehandlingStegType fra, BehandlingStegType til) {
        var kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        return new BehandlingStegTilbakeføringEvent(kontekst,
            new BehandlingStegTilstandSnapshot(1L, fra, BehandlingStegStatus.TILBAKEFØRT),
            new BehandlingStegTilstandSnapshot(2L, til, BehandlingStegStatus.INNGANG));
    }

    private void verifiserIngenVekking() {
        verify(behandlingProsesseringTjeneste, never()).opprettTasksForGjenopptaOppdaterFortsett(any(), anyBoolean());
    }

    private void settPåVentIVentepunktForVedtaksprioritet(Behandling behandling) {
        new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_VEDTAKSPRIORITET, VURDER_VEDTAKSPRIORITET);
        lagreOgFlush(behandling);
    }

    private Behandling nyBehandling(Behandling original, BehandlingType type, BehandlingStegType aktivtSteg) {
        var behandling = Behandling.fraTidligereBehandling(original, type).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, aktivtSteg);
        lagreOgFlush(behandling);
        return behandling;
    }

    private void lagreOgFlush(Behandling behandling) {
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
    }
}
