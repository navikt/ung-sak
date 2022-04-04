package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface YtelsesspesifikkRelasjonsFilter {
    boolean relasjonsFiltreringBarn(Behandling behandling, Personinfo barn);

    boolean hentHistorikkForRelatertePersoner();
}
