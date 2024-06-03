package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class SykdomGrunnlagSammenlikningsresultat {
    LocalDateTimeline<Boolean> diffPerioder;
    private boolean endretDiagnosekoder;

    public SykdomGrunnlagSammenlikningsresultat(LocalDateTimeline<Boolean> diffPerioder, boolean endretDiagnosekoder) {
        this.diffPerioder = diffPerioder;
        this.endretDiagnosekoder = endretDiagnosekoder;
    }

    public LocalDateTimeline<Boolean> getDiffPerioder() {
        return diffPerioder;
    }

    public boolean isEndretDiagnosekoder() {
        return endretDiagnosekoder;
    }

    public boolean harBlittEndret() {
        return endretDiagnosekoder || !diffPerioder.isEmpty();
    }
}
