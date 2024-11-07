package no.nav.k9.sak.behandlingskontroll.impl.transisjoner;

import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingskontroll.transisjoner.StegTransisjon;

class Utført implements StegTransisjon {

    @Override
    public String getId() {
        return FellesTransisjoner.UTFØRT.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        return nåværendeSteg.getBehandlingModell().finnNesteSteg(nåværendeSteg.getBehandlingStegType());
    }

    @Override
    public String toString() {
        return "Utført{}";
    }
}
