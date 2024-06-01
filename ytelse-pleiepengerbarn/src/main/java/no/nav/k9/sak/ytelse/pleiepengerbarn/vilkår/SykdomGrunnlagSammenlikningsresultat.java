package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class SykdomGrunnlagSammenlikningsresultat {
    LocalDateTimeline<Boolean> diffPerioder;
    private boolean endretDiagnosekoder;
    private final boolean harEndretAntallSykdomsdokumenter;

    public SykdomGrunnlagSammenlikningsresultat(LocalDateTimeline<Boolean> diffPerioder, boolean endretDiagnosekoder, boolean harEndretAntallSykdomsdokumenter) {
        this.diffPerioder = diffPerioder;
        this.endretDiagnosekoder = endretDiagnosekoder;
        this.harEndretAntallSykdomsdokumenter = harEndretAntallSykdomsdokumenter;
    }

    public LocalDateTimeline<Boolean> getDiffPerioder() {
        return diffPerioder;
    }

    public boolean isEndretDiagnosekoder() {
        return endretDiagnosekoder;
    }

    public boolean isHarEndretAntallSykdomsdokumenter() {
        return harEndretAntallSykdomsdokumenter;
    }

    public boolean harBlittEndret() {
        return harEndretAntallSykdomsdokumenter || endretDiagnosekoder || !diffPerioder.isEmpty();
    }
}
