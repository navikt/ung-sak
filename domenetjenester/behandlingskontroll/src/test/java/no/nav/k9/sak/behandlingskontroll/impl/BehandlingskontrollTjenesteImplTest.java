package no.nav.k9.sak.behandlingskontroll.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingModellVisitor;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegUtfall;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.k9.sak.behandlingskontroll.testutilities.TestScenario;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingEvent;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.k9.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingskontrollTjenesteImplTest {

    @Inject
    private EntityManager entityManager;
    private BehandlingskontrollTjenesteImpl kontrollTjeneste;
    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;

    private BehandlingskontrollEventPublisererForTest eventPubliserer ;
    private BehandlingModellRepository behandlingModellRepository ;
    private BehandlingskontrollServiceProvider serviceProvider ;
    private InternalManipulerBehandling manipulerInternBehandling ;

    private BehandlingStegType steg2;
    private BehandlingStegType steg3;
    private BehandlingStegType steg4;
    private BehandlingStegType steg5;

    private BehandlingModell modell;

    private String steg2InngangAksjonspunkt;

    private String steg2UtgangAksjonspunkt;

    @SuppressWarnings("resource")
    @BeforeEach
    public void setup() {

        eventPubliserer = new BehandlingskontrollEventPublisererForTest();
        behandlingModellRepository = new BehandlingModellRepository();
        serviceProvider = new BehandlingskontrollServiceProvider(entityManager, behandlingModellRepository, eventPubliserer);
        manipulerInternBehandling = new InternalManipulerBehandling();

        TestScenario scenario = TestScenario.dummyScenario();
        behandling = scenario.lagre(serviceProvider);
        modell = serviceProvider.getBehandlingModellRepository().getModell(behandling.getType(), behandling.getFagsakYtelseType());

        steg2 = BehandlingStegType.KONTROLLER_FAKTA;
        steg3 = modell.finnNesteSteg(steg2).getBehandlingStegType();
        steg4 = modell.finnNesteSteg(steg3).getBehandlingStegType();
        steg5 = modell.finnNesteSteg(steg4).getBehandlingStegType();
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, steg3);

        steg2InngangAksjonspunkt = modell.finnAksjonspunktDefinisjonerInngang(steg2).iterator().next();
        steg2UtgangAksjonspunkt = modell.finnAksjonspunktDefinisjonerUtgang(steg2).iterator().next();

        initBehandlingskontrollTjeneste();

        kontekst = Mockito.mock(BehandlingskontrollKontekst.class);
        Mockito.when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        Mockito.when(kontekst.getFagsakId()).thenReturn(behandling.getFagsakId());
    }

    @Test
    public void skal_rykke_tilbake_til_inngang_vurderingspunkt_av_steg() {

        BehandlingStegType steg = steg2;
        String inngangAksjonspunkt = steg2InngangAksjonspunkt;

        kontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, List.of(inngangAksjonspunkt));

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();
        assertThat(getBehandlingStegTilstand(behandling)).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.TILBAKEFØRT);
        sjekkBehandlingStegTilstandHistorikk(behandling, steg2,
            BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_rykke_tilbake_til_utgang_vurderingspunkt_av_steg() {

        kontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, List.of(steg2UtgangAksjonspunkt));

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg2);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.UTGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstand(steg2)).isPresent();

        entityManager.persist(behandling); // lagre for å sjekke BehandlingStegTilstand i db.
        entityManager.flush();

        assertThat(getBehandlingStegTilstand(behandling)).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.TILBAKEFØRT);
        sjekkBehandlingStegTilstandHistorikk(behandling, steg2,
            BehandlingStegStatus.UTGANG);

    }

    @Test
    public void skal_rykke_tilbake_til_start_av_tidligere_steg_ved_tilbakeføring() {

        kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, steg2);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg2);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstand(steg2)).isPresent();
        assertThat(getBehandlingStegTilstand(behandling)).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.TILBAKEFØRT);
        sjekkBehandlingStegTilstandHistorikk(behandling, steg2,
            BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_tolerere_tilbakehopp_til_senere_steg_enn_inneværende() {

        kontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, steg4);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg3);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isNull();
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        entityManager.persist(behandling);
        entityManager.flush();

        assertThat(getBehandlingStegTilstand(behandling)).hasSize(1);

        assertThat(behandling.getBehandlingStegTilstand(steg3)).isPresent();
        assertThat(behandling.getBehandlingStegTilstand(steg4)).isNotPresent();

    }

    @Test
    public void skal_flytte_til__inngang_av_senere_steg_ved_framføring() {

        kontrollTjeneste.behandlingFramføringTilSenereBehandlingSteg(kontekst, steg5);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg5);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(behandling.getBehandlingStegTilstand(steg5)).isPresent();
        assertThat(getBehandlingStegTilstand(behandling)).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg3,
            BehandlingStegStatus.AVBRUTT);

        // NB: skipper STEP_4
        sjekkBehandlingStegTilstandHistorikk(behandling, steg4);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg5,
            BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_kaste_exception_dersom_tilbakeføring_til_senere_steg() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, steg4);
        });
    }

    @Test
    public void skal_kaste_exception_dersom_ugyldig_tilbakeføring_fra_iverks() {
        // Arrange
        BehandlingStegType iverksettSteg = BehandlingStegType.IVERKSETT_VEDTAK;
        BehandlingStegType forrigeSteg = modell.finnForrigeSteg(iverksettSteg).getBehandlingStegType();
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, iverksettSteg);

        // Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {

            // Act
            kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, forrigeSteg);

        });
    }

    @Test
    public void skal_rykke_tilbake_til_inngang_vurderingspunkt_av_samme_steg() {

        // Arrange
        var steg = steg2;
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, steg, BehandlingStegStatus.UTGANG, BehandlingStegStatus.AVBRUTT);

        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.UTGANG);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);

        // Act
        kontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, List.of(steg2InngangAksjonspunkt));

        // Assert
        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(steg);
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.UTREDES);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(behandling.getBehandlingStegTilstand()).isNotNull();

        assertThat(getBehandlingStegTilstand(behandling)).hasSize(2);

        sjekkBehandlingStegTilstandHistorikk(behandling, steg, BehandlingStegStatus.INNGANG);

        assertThat(behandling.getBehandlingStegTilstand(steg).get().getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);

    }

    @Test
    public void skal_ha_guard_mot_nøstet_behandlingskontroll_ved_prossesering_tilbakeføring_og_framføring() throws Exception {

        this.kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider) {
            @Override
            protected BehandlingStegUtfall doProsesserBehandling(BehandlingskontrollKontekst kontekst, BehandlingModell modell,
                                                                 BehandlingModellVisitor visitor) {
                kontrollTjeneste.prosesserBehandling(kontekst);
                return null;
            }
        };

        Assertions.assertThrows(IllegalStateException.class, () -> {
            this.kontrollTjeneste.prosesserBehandling(kontekst);
        });
    }

    @Test
    public void skal_returnere_true_når_aksjonspunktet_skal_løses_i_angitt_steg() {
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(), behandling.getType(), StartpunktType.KONTROLLER_FAKTA,
            steg3.getAksjonspunktDefinisjonerUtgang().get(0)))
                .isTrue();
    }

    @Test
    public void skal_returnere_false_når_aksjonspunktet_skal_løses_før_angitt_steg() {
        assertThat(kontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(), behandling.getType(), StartpunktType.BEREGNING,
            steg2.getAksjonspunktDefinisjonerUtgang().get(0)))
                .isFalse();
    }

    private void sjekkBehandlingStegTilstandHistorikk(Behandling behandling, BehandlingStegType stegType,
                                                      BehandlingStegStatus... stegStatuser) {

        var stegTilstander = getBehandlingStegTilstand(behandling);
        assertThat(
            stegTilstander.stream()
                .filter(bst -> stegType == null || Objects.equals(bst.getBehandlingSteg(), stegType))
                .map(bst -> bst.getBehandlingStegStatus()))
                    .containsExactly(stegStatuser);
    }

    @SuppressWarnings("unchecked")
    private List<BehandlingStegTilstand> getBehandlingStegTilstand(Behandling behandling) {
        return entityManager.createNativeQuery("select s.* from behandling_steg_tilstand s where s.behandling_id=:id", BehandlingStegTilstand.class)
            .setParameter("id", behandling.getId()).getResultList();
    }

    private void initBehandlingskontrollTjeneste() {
        this.kontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
    }

    private final class BehandlingskontrollEventPublisererForTest extends BehandlingskontrollEventPubliserer {
        private List<BehandlingEvent> events = new ArrayList<>();

        @Override
        protected void doFireEvent(BehandlingEvent event) {
            events.add(event);
        }
    }

}
