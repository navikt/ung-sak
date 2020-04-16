package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import java.time.LocalDate;
import java.util.Objects;

public final class InfotrygdFeedPeriode {
    private final LocalDate fom;
    private final LocalDate tom;

    public InfotrygdFeedPeriode(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public static InfotrygdFeedPeriode annullert() {
        return new InfotrygdFeedPeriode(null, null);
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    @Override
    public String toString() {
        return "InfotrygdFeedPeriode{" +
            "fom=" + fom +
            ", tom=" + tom +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InfotrygdFeedPeriode that = (InfotrygdFeedPeriode) o;
        return Objects.equals(fom, that.fom) &&
            Objects.equals(tom, that.tom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom);
    }
}
