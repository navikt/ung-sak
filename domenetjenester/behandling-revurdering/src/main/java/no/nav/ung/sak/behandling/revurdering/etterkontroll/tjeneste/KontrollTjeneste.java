package no.nav.ung.sak.behandling.revurdering.etterkontroll.tjeneste;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.EtterkontrollRef;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.KontrollType;

public interface KontrollTjeneste {

    static KontrollTjeneste finnTjeneste(Instance<KontrollTjeneste> instances, FagsakYtelseType ytelseType, KontrollType kontrollType) {
        return EtterkontrollRef.Lookup.find(instances, ytelseType, kontrollType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + KontrollTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    /**
     * Tar inn en etterkontroll og returnerer om denne er behandlet ferdig
     *
     * @return er behandlet ferdig
     */
    boolean utfør(Etterkontroll etterkontroll);
}
