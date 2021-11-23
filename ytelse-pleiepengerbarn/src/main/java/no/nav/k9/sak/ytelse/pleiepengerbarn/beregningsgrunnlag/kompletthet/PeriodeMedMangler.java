package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.util.List;
import java.util.Objects;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;

public class PeriodeMedMangler {
    private DatoIntervallEntitet periode;
    private List<ManglendeVedlegg> mangler;

    public PeriodeMedMangler(DatoIntervallEntitet periode, List<ManglendeVedlegg> mangler) {
        this.periode = Objects.requireNonNull(periode);
        this.mangler = Objects.requireNonNull(mangler);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public List<ManglendeVedlegg> getMangler() {
        return mangler;
    }

    public boolean harMangler() {
        return !mangler.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodeMedMangler that = (PeriodeMedMangler) o;
        return Objects.equals(periode, that.periode) && Objects.equals(mangler, that.mangler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, mangler);
    }

    @Override
    public String toString() {
        return "PeriodeMedMangler{" +
            "periode=" + periode +
            ", mangler=" + mangler.size() +
            '}';
    }
}
