package no.nav.k9.sak.domene.registerinnhenting.personopplysninger;

import java.util.Optional;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public class PleietrengendeRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {
    public PleietrengendeRelasjonsFilter() {
    }

    @Override
    public boolean relasjonsFiltrering(Behandling behandling, Personinfo it) {
        final var pleietrengende = Optional.ofNullable(behandling.getFagsak().getPleietrengendeAktørId());
        if (pleietrengende.isEmpty()) {
            return false;
        }
        return it.getAktørId().equals(pleietrengende.orElse(null));
    }
}
