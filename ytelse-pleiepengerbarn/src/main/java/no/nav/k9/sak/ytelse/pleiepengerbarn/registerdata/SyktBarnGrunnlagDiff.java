package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.List;
import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.behandlingslager.diff.Node;
import no.nav.k9.sak.behandlingslager.diff.Pair;
import no.nav.k9.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;

public class SyktBarnGrunnlagDiff extends DiffResult {

    private SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat;
    private LocalDateTimeline<Boolean> perioderMedEndringIEtablertTilsyn;
    private List<DatoIntervallEntitet> nattevåkBeredskap;

    public SyktBarnGrunnlagDiff(SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat, LocalDateTimeline<Boolean> perioderMedEndringIEtablertTilsyn, List<DatoIntervallEntitet> nattevåkBeredskap) {
        super(null, null, null);
        this.grunnlagSammenlikningsresultat = grunnlagSammenlikningsresultat;
        this.perioderMedEndringIEtablertTilsyn = perioderMedEndringIEtablertTilsyn;
        this.nattevåkBeredskap = nattevåkBeredskap;
    }

    @Override
    public Map<Node, Pair> getLeafDifferences() {
        if (areDifferent()) {
            return new DiffEntity(TraverseEntityGraphFactory.build())
                .diff(null, new SykdomDiffData(grunnlagSammenlikningsresultat, perioderMedEndringIEtablertTilsyn, nattevåkBeredskap))
                .getLeafDifferences();
        }
        return Map.of();
    }

    @Override
    public boolean isEmpty() {
        var sykdomEmpty = grunnlagSammenlikningsresultat == null || grunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
        var tilsynEmpty = perioderMedEndringIEtablertTilsyn == null || perioderMedEndringIEtablertTilsyn.isEmpty();
        var nattevåkEmpty = nattevåkBeredskap == null || nattevåkBeredskap.isEmpty();

        return sykdomEmpty && tilsynEmpty && nattevåkEmpty;
    }

    @Override
    public boolean areDifferent() {
        var sykdomEndring = grunnlagSammenlikningsresultat != null && !grunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
        var tilsynEndring = perioderMedEndringIEtablertTilsyn != null && !perioderMedEndringIEtablertTilsyn.isEmpty();
        var nattevåkEndring = nattevåkBeredskap != null && !nattevåkBeredskap.isEmpty();
        return sykdomEndring || tilsynEndring || nattevåkEndring;
    }

    @Override
    public String toString() {
        return "SykdomDiffResult{" +
            "isEmpty=" + isEmpty() +
            ", areDifferent=" + areDifferent() +
            ", sykdomDiff=" + (grunnlagSammenlikningsresultat != null ? grunnlagSammenlikningsresultat.getDiffPerioder().toString() : "[]") +
            ", tilsynDiff=" + (perioderMedEndringIEtablertTilsyn != null ? perioderMedEndringIEtablertTilsyn.toString() : "[]") +
            ", nattevåkBeredskapDiff=" + (nattevåkBeredskap != null ? nattevåkBeredskap.toString() : "[]") +
            "}";
    }
}
