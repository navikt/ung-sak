package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public class OmsorgspengerRammevedtakRelasjonFilter implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public boolean relasjonsFiltreringBarn(Behandling behandling, Personinfo barn) {
        return true;
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return true;
    }
}
