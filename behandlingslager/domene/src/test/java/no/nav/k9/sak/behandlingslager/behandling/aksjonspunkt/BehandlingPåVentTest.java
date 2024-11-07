package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

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
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        assertThat(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErIkkePåVentEttInnslag() {
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }

    @Test
    // TODO PKMANTIS-1137 Har satt midlertidig frist, må endres når dynamisk frist er implementert
    public void testErPåVentNårVenterPåOpptjeningsopplysninger() {
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getOpprettetDato().plusWeeks(2).toLocalDate()).isEqualTo(aksjonspunkt.getFristTid().toLocalDate());
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }
}
