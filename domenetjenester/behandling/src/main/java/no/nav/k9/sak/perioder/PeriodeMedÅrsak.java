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
    public int compareTo(PeriodeMedÅrsak o) {
        return periode.compareTo(o.getPeriode());
    }
}
