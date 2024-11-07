package no.nav.k9.sak.behandlingskontroll.impl.transisjoner;

import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingskontroll.transisjoner.StegTransisjon;

class SettPåVent implements StegTransisjon {

    @Override
    public String getId() {
        return FellesTransisjoner.SETT_PÅ_VENT.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        return nåværendeSteg;
    }

    @Override
    public String toString() {
        return "SettPåVent{}";
    }
}
