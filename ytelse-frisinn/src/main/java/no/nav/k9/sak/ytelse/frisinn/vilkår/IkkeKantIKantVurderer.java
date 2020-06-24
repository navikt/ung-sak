package no.nav.k9.sak.ytelse.frisinn.vilkår;

import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class IkkeKantIKantVurderer implements KantIKantVurderer {
    public IkkeKantIKantVurderer() {
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

