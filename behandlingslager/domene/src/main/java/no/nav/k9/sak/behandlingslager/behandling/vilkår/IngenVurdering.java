package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

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

    @Override
    public boolean erKomprimerbar() {
        return false;
    }
}

