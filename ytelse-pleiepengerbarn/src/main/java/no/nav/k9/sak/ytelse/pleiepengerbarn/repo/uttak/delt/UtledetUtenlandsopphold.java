package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.delt;

import java.util.Objects;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;

public class UtledetUtenlandsopphold {

    private final Landkoder landkode;
    private final UtenlandsoppholdÅrsak årsak;

    public UtledetUtenlandsopphold(Landkoder landkode, UtenlandsoppholdÅrsak årsak) {
        this.landkode = landkode;
        this.årsak = årsak;
    }

    public Landkoder getLandkode() {
        return landkode;
    }

    public UtenlandsoppholdÅrsak getÅrsak() {
        return årsak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtledetUtenlandsopphold that = (UtledetUtenlandsopphold) o;
        return Objects.equals(landkode, that.landkode) && årsak == that.årsak;
    }

    @Override
    public int hashCode() {
        return Objects.hash(landkode, årsak);
    }
}
