package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public class DefaultRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {
    public DefaultRelasjonsFilter() {
    }

    @Override
    public boolean relasjonsFiltrering(Behandling behandling, Personinfo it) {
        return it.getAlder() <= 13;
    }
}
