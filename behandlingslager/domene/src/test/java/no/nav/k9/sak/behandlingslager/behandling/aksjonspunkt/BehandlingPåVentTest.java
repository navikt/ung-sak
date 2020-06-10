package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public class BehandlingPåVentTest {

    private Fagsak fagsak;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    private Behandling behandling;

    @Before
    public void setup() {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
    }

    @Test
    public void testErIkkePåVentUtenInnslag() {
        Assert.assertFalse(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErPåVentEttInnslag() {
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        Assert.assertTrue(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErIkkePåVentEttInnslag() {
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        Assert.assertFalse(behandling.isBehandlingPåVent());
    }

    @Test // TODO PKMANTIS-1137 Har satt midlertidig frist, må endres når dynamisk frist er implementert
    public void testErPåVentNårVenterPåOpptjeningsopplysninger() {
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER);
        Assert.assertTrue(behandling.isBehandlingPåVent());
        Assert.assertEquals(behandling.getOpprettetDato().plusWeeks(2).toLocalDate(), aksjonspunkt.getFristTid().toLocalDate());
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        Assert.assertFalse(behandling.isBehandlingPåVent());
    }
}
