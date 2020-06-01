package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class DefaultKantIKantVurderer implements KantIKantVurderer {
    public DefaultKantIKantVurderer() {
    }

    @Override
    public boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return periode2.grenserTil(periode1);
    }

    @Override
    public boolean erKomprimerbar() {
        return true;
    }
}
