package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

import no.nav.ung.sak.behandlingslager.behandling.sporing.Sporingsverdi;

import java.util.Objects;

public final class Barnetillegg extends Sporingsverdi {
    private final int dagsats;
    private final int antallBarn;

    public Barnetillegg(int dagsats, int antallBarn) {
        this.dagsats = dagsats;
        this.antallBarn = antallBarn;
    }

    @Override
    public String toString() {
        return "Barnetillegg{" +
            "dagsats=" + dagsats +
            ", antallBarn=" + antallBarn +
            '}';
    }

    @Override
    public String tilRegelVerdi() {
        return toString();
    }

    public int dagsats() {
        return dagsats;
    }

    public int antallBarn() {
        return antallBarn;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Barnetillegg) obj;
        return this.dagsats == that.dagsats &&
            this.antallBarn == that.antallBarn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dagsats, antallBarn);
    }

}
