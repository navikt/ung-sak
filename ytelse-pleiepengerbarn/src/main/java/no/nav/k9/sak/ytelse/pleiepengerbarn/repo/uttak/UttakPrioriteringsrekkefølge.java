package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class UttakPrioriteringsrekkefølge {

    private final boolean annenSakBehandlesFørst;
    private final LocalDateTimeline<Boolean> tidslinjeUtenPrioritet;

    public UttakPrioriteringsrekkefølge(boolean annenSakBehandlesFørst, LocalDateTimeline<Boolean> tidslinjeUtenPrioritet) {
        this.annenSakBehandlesFørst = annenSakBehandlesFørst;
        this.tidslinjeUtenPrioritet = tidslinjeUtenPrioritet;
    }

    public boolean isAnnenSakBehandlesFørst() {
        return annenSakBehandlesFørst;
    }

    public LocalDateTimeline<Boolean> getTidslinjeUtenPrioritet() {
        return tidslinjeUtenPrioritet;
    }

    public boolean harGjensidigAvhengighet() {
        return !annenSakBehandlesFørst && !tidslinjeUtenPrioritet.isEmpty();
    }

    @Override
    public String toString() {
        return "UttakPrioriteringsrekkefølge{" +
            "annenSakBehandlesFørst=" + annenSakBehandlesFørst +
            ", tidslinjeUtenPrioritet=" + tidslinjeUtenPrioritet +
            '}';
    }
}
