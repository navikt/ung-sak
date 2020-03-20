package no.nav.k9.sak.behandlingskontroll.impl.transisjoner;

import java.util.Optional;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.transisjoner.StegTransisjon;

/**
 * Hopper fremover fra steg A til steg B slik at stegene besøkes.
 *
 * NB! Merk at denne transisjonstypen besøker stegene slik at {@see BehandlingSteg#vedHoppOverFramover} kalles på.
 */
class FremoverhoppTransisjon implements StegTransisjon {

    private final String id;
    private final BehandlingStegType målsteg;

    public FremoverhoppTransisjon(String id, BehandlingStegType målsteg) {
        this.id = id;
        this.målsteg = målsteg;
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        Optional<BehandlingStegModell> funnetMålsteg = nåværendeSteg.getBehandlingModell().hvertStegEtter(nåværendeSteg.getBehandlingStegType())
            .filter(s -> s.getBehandlingStegType().equals(målsteg))
            .findFirst();
        if (funnetMålsteg.isPresent()) {
            return funnetMålsteg.get();
        }
        throw new IllegalArgumentException("Finnes ikke noe steg av type " + målsteg + " etter " + nåværendeSteg);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Optional<BehandlingStegType> getMålstegHvisFremoverhopp() {
        return Optional.of(målsteg);
    }

    @Override
    public String toString() {
        return "FremoverhoppTransisjon{" +
            "id='" + id + '\'' +
            '}';
    }
}
