package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;

class SykdomDiffData {

    private final SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat;
    private final LocalDateTimeline<Boolean> perioderMedEndringIEtablertTilsyn;
    private final List<DatoIntervallEntitet> nattevåkBeredskap;

    SykdomDiffData(SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat, LocalDateTimeline<Boolean> perioderMedEndringIEtablertTilsyn, List<DatoIntervallEntitet> nattevåkBeredskap) {
        this.grunnlagSammenlikningsresultat = grunnlagSammenlikningsresultat;
        this.perioderMedEndringIEtablertTilsyn = perioderMedEndringIEtablertTilsyn;
        this.nattevåkBeredskap = nattevåkBeredskap;
    }

    public SykdomGrunnlagSammenlikningsresultat getGrunnlagSammenlikningsresultat() {
        return grunnlagSammenlikningsresultat;
    }

    public LocalDateTimeline<Boolean> getPerioderMedEndringIEtablertTilsyn() {
        return perioderMedEndringIEtablertTilsyn;
    }

    public List<DatoIntervallEntitet> getNattevåkBeredskap() {
        return nattevåkBeredskap;
    }
}
