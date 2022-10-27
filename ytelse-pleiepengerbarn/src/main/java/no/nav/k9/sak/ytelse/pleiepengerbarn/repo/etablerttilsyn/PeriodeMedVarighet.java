package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import java.time.Duration;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class PeriodeMedVarighet {

    private final DatoIntervallEntitet periode;
    private final Duration varighet;
    
    public PeriodeMedVarighet(DatoIntervallEntitet periode, Duration varighet) {
        this.periode = periode;
        this.varighet = varighet;
    }
    
    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
    
    public Duration getVarighet() {
        return varighet;
    }
}
