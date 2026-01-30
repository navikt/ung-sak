package no.nav.ung.sak.behandlingslager.behandling.vilkår;

import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.tid.KantIKantVurderer;

/**
 * Ingen vurdering, alt er false (dvs. ikke kant i kant).
 */
public class IngenVurdering implements KantIKantVurderer {
    public IngenVurdering() {
    }

    @Override
    public boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return false;
    }
}

