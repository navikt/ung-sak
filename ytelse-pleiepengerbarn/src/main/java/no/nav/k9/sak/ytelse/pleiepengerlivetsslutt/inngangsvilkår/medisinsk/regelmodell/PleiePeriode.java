package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;
import java.util.Objects;


public class PleiePeriode {
    private final LocalDate fraOgMed;
    private final LocalDate tilOgMed;
    private final Pleielokasjon pleielokasjon;

    public PleiePeriode(LocalDate fom, LocalDate tilOgMed, Pleielokasjon pleielokasjon) {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(tilOgMed);
        Objects.requireNonNull(pleielokasjon);
        this.fraOgMed = fom;
        this.tilOgMed = tilOgMed;
        this.pleielokasjon = pleielokasjon;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public Pleielokasjon getPleielokasjon() {
        return pleielokasjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PleiePeriode periode = (PleiePeriode) o;
        return fraOgMed.equals(periode.fraOgMed) &&
            tilOgMed.equals(periode.tilOgMed) &&
            pleielokasjon == periode.pleielokasjon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraOgMed, tilOgMed, pleielokasjon);
    }

    @Override
    public String toString() {
        return "PleiePeriode{" +
            "fraOgMed=" + fraOgMed +
            ", tilOgMed=" + tilOgMed +
            ", pleielokasjon=" + pleielokasjon +
            '}';
    }
}
