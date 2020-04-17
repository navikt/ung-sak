package no.nav.k9.sak.infotrygdfeed;

import java.time.LocalDate;

public class TestPeriode {
    private final LocalDate fom;
    private final LocalDate tom;
    private final boolean innvilget;

    public TestPeriode(LocalDate fom, LocalDate tom, boolean innvilget) {
        this.fom = fom;
        this.tom = tom;
        this.innvilget = innvilget;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean isInnvilget() {
        return innvilget;
    }
}
