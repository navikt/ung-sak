package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface KantIKantVurderer {

    boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2);

    default boolean erKomprimerbar() {
        return true;
    }
}
