package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.time.Month;

import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * KantIKantVurderer som er laget for å aldri slå sammen vilkårsperioder.
 *
 */
public class IkkeKantIKantVurderer implements KantIKantVurderer {
    public IkkeKantIKantVurderer() {
    }

    @Override
    public boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return slutterIAprilEllerMai(periode1) && slutterIAprilEllerMai(periode2);
    }

    private boolean slutterIAprilEllerMai(DatoIntervallEntitet periode1) {
        return periode1.getTomDato().getMonth().equals(Month.APRIL) || periode1.getTomDato().getMonth().equals(Month.MAY);
    }

    @Override
    public boolean erKomprimerbar() {
        return false;
    }
}

