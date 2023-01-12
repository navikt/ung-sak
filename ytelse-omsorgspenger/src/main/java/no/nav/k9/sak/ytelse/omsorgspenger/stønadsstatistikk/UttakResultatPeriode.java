package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.util.Collections;
import java.util.List;

public class UttakResultatPeriode {
    private List<UttakAktivitet> uttakAktiviteter;

    public UttakResultatPeriode(List<UttakAktivitet> uttakAktiviteter) {
        if (uttakAktiviteter == null || uttakAktiviteter.isEmpty()) {
            this.uttakAktiviteter = Collections.emptyList();
        } else {
            this.uttakAktiviteter = Collections.unmodifiableList(uttakAktiviteter);
        }
    }

    public List<UttakAktivitet> getUttakAktiviteter() {
        return uttakAktiviteter;
    }

}
