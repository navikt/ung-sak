package no.nav.k9.sak.behandlingskontroll.impl.transisjoner;

import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingskontroll.transisjoner.StegTransisjon;

class TilbakeføringTilStegTransisjon implements StegTransisjon {

    @Override
    public String getId() {
        return FellesTransisjoner.TILBAKEFØRT_TIL_STEG.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        throw new IllegalArgumentException("Utvikler-feil: skal ikke kalle nesteSteg på " + getId());
    }

    @Override
    public String toString() {
        return "TilbakeføringTilStegTransisjon";
    }
}
