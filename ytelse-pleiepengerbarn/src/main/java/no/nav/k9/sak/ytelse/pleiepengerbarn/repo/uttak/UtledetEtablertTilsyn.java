package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;
import java.util.Objects;

import no.nav.k9.sak.kontrakt.tilsyn.Kilde;

public class UtledetEtablertTilsyn {

    private Duration varighet;
    private Kilde kilde;


    public UtledetEtablertTilsyn(Duration varighet, Kilde kilde) {
        this.varighet = varighet;
        this.kilde = kilde;
    }

    public Duration getVarighet() {
        return varighet;
    }

    public Kilde getKilde() {
        return kilde;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtledetEtablertTilsyn that = (UtledetEtablertTilsyn) o;
        return Objects.equals(varighet, that.varighet) && kilde == that.kilde;
    }

    @Override
    public int hashCode() {
        return Objects.hash(varighet, kilde);
    }
}
