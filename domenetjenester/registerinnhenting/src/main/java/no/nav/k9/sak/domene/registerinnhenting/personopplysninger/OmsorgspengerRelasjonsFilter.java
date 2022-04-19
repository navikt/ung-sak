package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.Periode;

public class OmsorgspengerRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public boolean relasjonsFiltreringBarn(Behandling behandling, Personinfo barn, Periode opplysningsperioden) {
        return barn.getAlder(opplysningsperioden.getFom()) <= 13;
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return true;
    }

    @Override
    public boolean hentDeltBosted() {
        return true;
    }
}
