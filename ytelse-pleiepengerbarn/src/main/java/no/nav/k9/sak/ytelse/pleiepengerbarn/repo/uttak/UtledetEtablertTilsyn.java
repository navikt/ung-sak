package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;

import no.nav.k9.sak.kontrakt.tilsyn.Kilde;

public class UtledetEtablertTilsyn {

    private Duration varighet;
    private Kilde kilde;

    
    public UtledetEtablertTilsyn(Duration varighet, Kilde kilde) {
        this.varighet = varighet;
        this.kilde = kilde;
    }

    
    public Duration getVarighet() {
        return varighet;
    }
    
    public Kilde getKilde() {
        return kilde;
    }
}
