package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.Periode;

public class AlleBarnOgTaMedHistorikk implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public boolean relasjonsFiltreringBarn(Behandling behandling, Personinfo barn, Periode opplysningsperioden) {
        return true;
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return true;
    }
}
