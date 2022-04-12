package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;
import java.util.Objects;


public class LivetsSluttfaseDokumentasjonPeriode {
    private final LocalDate fraOgMed;
    private final LocalDate tilOgMed;
    private final LivetsSluttfaseDokumentasjon dokumentasjon;

    public LivetsSluttfaseDokumentasjonPeriode(LocalDate fom, LocalDate tilOgMed, LivetsSluttfaseDokumentasjon pleielokasjon) {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(tilOgMed);
        Objects.requireNonNull(pleielokasjon);
        this.fraOgMed = fom;
        this.tilOgMed = tilOgMed;
        this.dokumentasjon = pleielokasjon;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public LivetsSluttfaseDokumentasjon getLivetsSluttfaseDokumentasjon() {
        return dokumentasjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LivetsSluttfaseDokumentasjonPeriode periode = (LivetsSluttfaseDokumentasjonPeriode) o;
        return fraOgMed.equals(periode.fraOgMed) &&
            tilOgMed.equals(periode.tilOgMed) &&
            dokumentasjon == periode.dokumentasjon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraOgMed, tilOgMed, dokumentasjon);
    }

    @Override
    public String toString() {
        return "LivetsSluttfaseDokumentasjonPeriode{" +
            "fraOgMed=" + fraOgMed +
            ", tilOgMed=" + tilOgMed +
            ", pleielokasjon=" + dokumentasjon +
            '}';
    }
}
