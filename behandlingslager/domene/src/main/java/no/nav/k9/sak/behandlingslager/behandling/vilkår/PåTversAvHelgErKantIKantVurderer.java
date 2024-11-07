package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.time.DayOfWeek;
import java.time.LocalDate;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class PåTversAvHelgErKantIKantVurderer implements KantIKantVurderer {
    public PåTversAvHelgErKantIKantVurderer() {
    }

    @Override
    public boolean erKantIKant(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return utledTomDato(periode1).equals(utledFom(periode2).minusDays(1)) || utledTomDato(periode2).equals(utledFom(periode1).minusDays(1));
    }

    private LocalDate utledFom(DatoIntervallEntitet periode1) {
        var fomDato = periode1.getFomDato();
        if (DayOfWeek.SATURDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(2);
        } else if (DayOfWeek.SUNDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(1);
        }
        return fomDato;
    }

    private LocalDate utledTomDato(DatoIntervallEntitet periode1) {
        var tomDato = periode1.getTomDato();
        if (DayOfWeek.FRIDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(2);
        } else if (DayOfWeek.SATURDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(1);
        }
        return tomDato;
    }
}
