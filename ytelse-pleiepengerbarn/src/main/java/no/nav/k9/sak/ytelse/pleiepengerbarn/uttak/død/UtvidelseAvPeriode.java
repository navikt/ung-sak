package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import java.time.temporal.TemporalUnit;
import java.util.Objects;

public class UtvidelseAvPeriode {

    private final int antall;
    private final TemporalUnit enhet;

    public UtvidelseAvPeriode(int antall, TemporalUnit enhet) {
        this.antall = antall;
        this.enhet = Objects.requireNonNull(enhet);
    }

    public int getAntall() {
        return antall;
    }

    public TemporalUnit getEnhet() {
        return enhet;
    }
}
