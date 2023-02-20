package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Pleiegrad;

public class LangvarigSykdomPeriode {
    private final LocalDate fraOgMed;
    private final LocalDate tilOgMed;
    private final LangvarigSykdomDokumentasjon dokumentasjon;

    public LangvarigSykdomPeriode(LocalDate fom, LocalDate tilOgMed, LangvarigSykdomDokumentasjon dokumentasjon) {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(tilOgMed);
        Objects.requireNonNull(dokumentasjon);
        this.fraOgMed = fom;
        this.tilOgMed = tilOgMed;
        this.dokumentasjon = dokumentasjon;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public LangvarigSykdomDokumentasjon getLangvarigSykdomDokumentasjon() {
        return dokumentasjon;
    }

    public Pleiegrad getGrad() {
        //TODO: Noe mer fornuftig
        if (dokumentasjon == LangvarigSykdomDokumentasjon.IKKE_DOKUMENTERT) {
            return Pleiegrad.INGEN;
        }
        return Pleiegrad.KONTINUERLIG_TILSYN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LangvarigSykdomPeriode periode = (LangvarigSykdomPeriode) o;
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
        return "LangvarigSykdomPeriode{" +
            "fraOgMed=" + fraOgMed +
            ", tilOgMed=" + tilOgMed +
            ", dokumentasjon=" + dokumentasjon +
            '}';
    }
}
