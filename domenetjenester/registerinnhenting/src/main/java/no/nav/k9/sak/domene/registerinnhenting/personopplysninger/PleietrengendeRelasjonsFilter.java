package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import java.util.Optional;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.Periode;

public class PleietrengendeRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public boolean relasjonsFiltreringBarn(Behandling behandling, Personinfo barn, Periode opplysningsperioden) {
        final var pleietrengende = Optional.ofNullable(behandling.getFagsak().getPleietrengendeAktørId());
        if (pleietrengende.isEmpty()) {
            return false;
        }
        return barn.getAktørId().equals(pleietrengende.orElse(null));
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return false;
    }

    @Override
    public boolean hentDeltBosted() {
        return false;
    }
}
