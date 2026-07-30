package no.nav.ung.sak.behandling.hendelse;

import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;

public class FinnAnsvarligSaksbehandler {

    private static final String DEFAULT_ANSVARLIG_SAKSBEHANDLER = "VL";

    private FinnAnsvarligSaksbehandler() {
        // hide public contructor
    }

    public static String finn(BehandlingAnsvarlig behandlingAnsvarlig) {
        if (behandlingAnsvarlig != null && behandlingAnsvarlig.getAnsvarligBeslutter() != null && !behandlingAnsvarlig.getAnsvarligBeslutter().isBlank()) {
            return behandlingAnsvarlig.getAnsvarligBeslutter();
        } else if (behandlingAnsvarlig != null && behandlingAnsvarlig.getAnsvarligSaksbehandler() != null && !behandlingAnsvarlig.getAnsvarligSaksbehandler().isBlank()) {
            return behandlingAnsvarlig.getAnsvarligSaksbehandler();
        }
        return DEFAULT_ANSVARLIG_SAKSBEHANDLER;
    }
}
