package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public class AlleRelasjonFilter implements YtelsesspesifikkRelasjonsFilter {
    public AlleRelasjonFilter() {
    }

    @Override
    public boolean relasjonsFiltrering(Behandling behandling, Personinfo it) {
        return true;
    }
}
