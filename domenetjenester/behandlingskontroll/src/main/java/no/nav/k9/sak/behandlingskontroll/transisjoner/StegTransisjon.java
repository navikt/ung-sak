package no.nav.k9.sak.behandlingskontroll.transisjoner;

import java.util.Optional;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;

public interface StegTransisjon {
    String getId();

    BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg);

    default Optional<BehandlingStegType> getMålstegHvisFremoverhopp() {
        return Optional.empty();
    }
}
