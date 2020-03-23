package no.nav.k9.sak.domene.iay.modell;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class ArbeidsforholdOverstyrtePerioder implements IndexKey {

    private DatoIntervallEntitet periode;

    @JsonBackReference
    private ArbeidsforholdOverstyring arbeidsforholdOverstyring;

    ArbeidsforholdOverstyrtePerioder() {
    }

    ArbeidsforholdOverstyrtePerioder(ArbeidsforholdOverstyrtePerioder arbeidsforholdOverstyrtePerioder) {
        this.periode = arbeidsforholdOverstyrtePerioder.getOverstyrtePeriode();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { getOverstyrtePeriode() };
        return IndexKeyComposer.createKey(keyParts);
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public ArbeidsforholdOverstyring getArbeidsforholdOverstyring() {
        return arbeidsforholdOverstyring;
    }

    void setArbeidsforholdOverstyring(ArbeidsforholdOverstyring arbeidsforholdOverstyring) {
        this.arbeidsforholdOverstyring = arbeidsforholdOverstyring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ArbeidsforholdOverstyrtePerioder)) return false;
        ArbeidsforholdOverstyrtePerioder that = (ArbeidsforholdOverstyrtePerioder) o;
        return Objects.equals(periode, that.periode) && Objects.equals(arbeidsforholdOverstyring, that.arbeidsforholdOverstyring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidsforholdOverstyring);
    }

    @Override
    public String toString() {
        return "ArbeidsforholdInformasjonEntitet{" +
            "periode=" + periode +
            ", arbeidsforholdOverstyring=" + arbeidsforholdOverstyring +
            '}';
    }

    public DatoIntervallEntitet getOverstyrtePeriode() {
        return periode;
    }
}
