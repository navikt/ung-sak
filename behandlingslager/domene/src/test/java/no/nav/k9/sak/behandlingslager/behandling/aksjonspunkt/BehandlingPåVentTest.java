package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public class BehandlingPåVentTest {

    private Fagsak fagsak;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    @Before
    public void setup() {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
    }

    @Test
    public void testErIkkePåVentUtenInnslag() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Assert.assertFalse(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErPåVentEttInnslag() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        Assert.assertTrue(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErIkkePåVentEttInnslag() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        Assert.assertFalse(behandling.isBehandlingPåVent());
    }

    @Test // TODO PKMANTIS-1137 Har satt midlertidig frist, må endres når dynamisk frist er implementert
    public void testErPåVentNårVenterPåOpptjeningsopplysninger() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER);
        Assert.assertTrue(behandling.isBehandlingPåVent());
        Assert.assertEquals(behandling.getOpprettetDato().plusWeeks(2).toLocalDate(), aksjonspunkt.getFristTid().toLocalDate());
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        Assert.assertFalse(behandling.isBehandlingPåVent());
    }
}
