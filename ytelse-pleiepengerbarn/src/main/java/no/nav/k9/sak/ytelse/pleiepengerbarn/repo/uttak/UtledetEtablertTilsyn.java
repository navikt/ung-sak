package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;

public class UtledetEtablertTilsyn {

    private Duration varighet;

    
    public UtledetEtablertTilsyn(Duration varighet) {
        this.varighet = varighet;
    }

    
    public Duration getVarighet() {
        return varighet;
    }
}
