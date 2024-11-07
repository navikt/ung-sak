package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import no.nav.k9.kodeverk.uttak.Tid;

import java.time.LocalDate;
import java.util.Objects;

public final class InfotrygdFeedPeriode {
    private final LocalDate fom;
    private final LocalDate tom;

    public InfotrygdFeedPeriode(LocalDate fom, LocalDate tom) {
        this.fom = fjernMinMaxVerdier(fom);
        this.tom = fjernMinMaxVerdier(tom);
        valider();
    }

    public static InfotrygdFeedPeriode annullert() {
        return new InfotrygdFeedPeriode(null, null);
    }

    private LocalDate fjernMinMaxVerdier(LocalDate dato) {
        if(Objects.equals(dato, Tid.TIDENES_BEGYNNELSE) || Objects.equals(dato, Tid.TIDENES_ENDE)) {
            return null;
        } else {
            return dato;
        }
    }

    private void valider() {
        if(fom != null && tom != null && tom.isBefore(fom)) {
            throw new IllegalArgumentException("Tom-dato kan ikke være før fom-dato.");
        }

        if(fom == null && tom != null) {
            throw new IllegalArgumentException("Kan ikke ha en tom-dato dersom fom-datoen er null.");
        }
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
