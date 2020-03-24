package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;
import java.util.Objects;

public class PleiePeriode {
    private final LocalDate fraOgMed;
    private final LocalDate tilOgMed;
    private final Pleiegrad grad;

    public PleiePeriode(LocalDate fom, LocalDate tilOgMed, Pleiegrad grad) {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(tilOgMed);
        Objects.requireNonNull(grad);
        this.fraOgMed = fom;
        this.tilOgMed = tilOgMed;
        this.grad = grad;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public Pleiegrad getGrad() {
        return grad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PleiePeriode periode = (PleiePeriode) o;
        return fraOgMed.equals(periode.fraOgMed) &&
            tilOgMed.equals(periode.tilOgMed) &&
            grad == periode.grad;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraOgMed, tilOgMed, grad);
    }

    @Override
    public String toString() {
        return "PleiePeriode{" +
            "fraOgMed=" + fraOgMed +
            ", tilOgMed=" + tilOgMed +
            ", grad=" + grad +
            '}';
    }
}
