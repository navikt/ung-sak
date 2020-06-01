package no.nav.k9.sak.ytelse.frisinn.vilkår;

import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class IkkeKantTilKant implements KantIKantVurderer {
    public IkkeKantTilKant() {
    }

    @Override
    public boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return false;
    }

}
