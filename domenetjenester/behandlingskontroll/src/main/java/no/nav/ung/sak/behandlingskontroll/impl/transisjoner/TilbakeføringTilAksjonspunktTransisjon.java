package no.nav.ung.sak.behandlingskontroll.impl.transisjoner;

import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingskontroll.transisjoner.StegTransisjon;

class TilbakeføringTilAksjonspunktTransisjon implements StegTransisjon {

    @Override
    public String getId() {
        return FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        throw new IllegalArgumentException("Utvikler-feil: skal ikke kalle nesteSteg på " + getId());
    }

    @Override
    public String toString() {
        return "TilbakeføringTilAksjonspunktTransisjon";
    }
}
