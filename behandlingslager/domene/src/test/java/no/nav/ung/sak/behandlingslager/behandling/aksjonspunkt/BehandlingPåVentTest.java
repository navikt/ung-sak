package no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_BOSTEDUTTALELSE;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;
import static org.assertj.core.api.Assertions.assertThat;

public class BehandlingPåVentTest {

    private Fagsak fagsak;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
    }

    @Test
    public void testErIkkePåVentUtenInnslag() {
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }

    @Test
    public void testErPåVentEttInnslag() {
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_SATT_PÅ_VENT_REVURDERING);
        assertThat(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErIkkePåVentEttInnslag() {
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_SATT_PÅ_VENT_REVURDERING);
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }

    @Test
    public void testErPåVentNårVenterPåOpptjeningsopplysninger() {
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_SATT_PÅ_VENT_REVURDERING);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getOpprettetDato().plusWeeks(2).toLocalDate()).isEqualTo(aksjonspunkt.getFristTid().toLocalDate());
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }

    @Test
    public void skal_være_på_vent_når_nav_kontor_autopunkt_er_eneste_åpne_aksjonspunkt() {
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_SATT_PÅ_VENT_ETTERLYST_BOSTEDUTTALELSE);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }
}
