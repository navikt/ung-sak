package no.nav.ung.sak.behandlingslager.behandling.vilkår;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public class DefaultKantIKantVurderer implements KantIKantVurderer {
    public DefaultKantIKantVurderer() {
    }

    @Override
    public boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return periode2.grenserTil(periode1);
    }
}
