package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class SykdomGrunnlagSammenlikningsresultat {
    LocalDateTimeline<Boolean> diffPerioder;
    private boolean endretDiagnosekoder;
    private boolean harNyeUklassifiserteDokumenter;

    public SykdomGrunnlagSammenlikningsresultat(LocalDateTimeline<Boolean> diffPerioder, boolean endretDiagnosekoder, boolean harNyeUklassifiserteDokumenter) {
        this.diffPerioder = diffPerioder;
        this.endretDiagnosekoder = endretDiagnosekoder;
        this.harNyeUklassifiserteDokumenter = harNyeUklassifiserteDokumenter;
    }

    public LocalDateTimeline<Boolean> getDiffPerioder() {
        return diffPerioder;
    }

    public boolean isEndretDiagnosekoder() {
        return endretDiagnosekoder;
    }

    public boolean harBlittEndret() {
        return harNyeUklassifiserteDokumenter || endretDiagnosekoder || !diffPerioder.isEmpty();
    }

    public boolean harNyeUklassifiserteDokumenter() {
        return harNyeUklassifiserteDokumenter;
    }
}
