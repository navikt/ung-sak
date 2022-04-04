package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import java.time.LocalDate;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public class OmsorgspengerRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public boolean relasjonsFiltreringBarn(Behandling behandling, Personinfo barn) {
        //FIXME sett dato som ikke er avhengig av nåtidspunkt, for eksempel Fagsak.periode.fom
        return barn.getAlder(LocalDate.now()) <= 13;
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return true;
    }
}
