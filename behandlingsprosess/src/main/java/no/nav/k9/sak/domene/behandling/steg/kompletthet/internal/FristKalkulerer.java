package no.nav.k9.sak.domene.behandling.steg.kompletthet.internal;

import java.time.LocalDateTime;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

final class FristKalkulerer {

    private FristKalkulerer() {
    }

    static LocalDateTime regnUtFrist(AksjonspunktDefinisjon definisjon, LocalDateTime eksisterendeFrist) {
        if (eksisterendeFrist != null) {
            return eksisterendeFrist;
        }
        if (definisjon.getFristPeriod() == null) {
            throw new IllegalArgumentException("[Utvikler feil] Prøver å utlede frist basert på et aksjonspunkt uten fristperiode definert");
        }

        return LocalDateTime.now().plus(definisjon.getFristPeriod());
    }
}
