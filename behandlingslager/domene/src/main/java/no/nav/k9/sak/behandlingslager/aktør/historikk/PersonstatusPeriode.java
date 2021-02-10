package no.nav.k9.sak.behandlingslager.aktør.historikk;

import java.util.Objects;

import no.nav.k9.kodeverk.person.PersonstatusType;

public class PersonstatusPeriode {

    private Gyldighetsperiode gyldighetsperiode;
    private PersonstatusType personstatus;

    public PersonstatusPeriode(Gyldighetsperiode gyldighetsperiode, PersonstatusType personstatus) {
        this.gyldighetsperiode = gyldighetsperiode;
        this.personstatus = personstatus;
    }

    public static boolean fuzzyEquals(PersonstatusPeriode p1, PersonstatusPeriode p2) {
        return Gyldighetsperiode.fuzzyEquals(p1.gyldighetsperiode, p2.gyldighetsperiode) &&
            Objects.equals(p1.personstatus, p2.personstatus);
    }


    public Gyldighetsperiode getGyldighetsperiode() {
        return this.gyldighetsperiode;
    }

    public PersonstatusType getPersonstatus() {
        return this.personstatus;
    }

    @Override
    public String toString() {
        return "PersonstatusPeriode(gyldighetsperiode=" + this.getGyldighetsperiode()
            + ", personstatus=" + this.getPersonstatus()
            + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonstatusPeriode that = (PersonstatusPeriode) o;
        return Objects.equals(gyldighetsperiode, that.gyldighetsperiode) &&
            Objects.equals(personstatus, that.personstatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gyldighetsperiode, personstatus);
    }
}
