package no.nav.ung.sak.behandlingslager.behandling.vilkår;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public interface KantIKantVurderer {

    boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2);

    default boolean erKomprimerbar() {
        return true;
    }
}
