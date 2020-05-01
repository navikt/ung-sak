package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public class OmsorgspengerRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {
    public OmsorgspengerRelasjonsFilter() {
    }

    @Override
    public boolean relasjonsFiltrering(Behandling behandling, Personinfo it) {
        return it.getAlder() <= 13;
    }
}
