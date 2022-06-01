package no.nav.k9.sak.perioder;

import java.util.Objects;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class PeriodeMedÅrsak implements Comparable<PeriodeMedÅrsak> {

    private DatoIntervallEntitet periode;
    private BehandlingÅrsakType årsak;

    public PeriodeMedÅrsak(DatoIntervallEntitet periode, BehandlingÅrsakType årsak) {
        this.periode = Objects.requireNonNull(periode);
        this.årsak = årsak;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public BehandlingÅrsakType getÅrsak() {
        return årsak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodeMedÅrsak that = (PeriodeMedÅrsak) o;
        return Objects.equals(periode, that.periode) && årsak == that.årsak;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, årsak);
    }

    @Override
    public int compareTo(PeriodeMedÅrsak o) {
        var periodeCompare = periode.compareTo(o.periode);
        if (periodeCompare == 0) {
            return årsak.compareTo(o.årsak);
        }
        return periodeCompare;
    }
}
