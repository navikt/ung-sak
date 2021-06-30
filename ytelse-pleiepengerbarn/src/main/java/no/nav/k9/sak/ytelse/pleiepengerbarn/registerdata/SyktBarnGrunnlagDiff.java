package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.behandlingslager.diff.Node;
import no.nav.k9.sak.behandlingslager.diff.Pair;
import no.nav.k9.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraph;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;

public class SyktBarnGrunnlagDiff extends DiffResult {

    private SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat;
    private LocalDateTimeline<Boolean> perioderMedEndringIEtablertTilsyn;

    public SyktBarnGrunnlagDiff(TraverseGraph traverser, TraverseGraph.TraverseResult result1, TraverseGraph.TraverseResult result2) {
        super(traverser, result1, result2);
    }

    public SyktBarnGrunnlagDiff(SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat, LocalDateTimeline<Boolean> perioderMedEndringIEtablertTilsyn) {
        this(null, null, null);
        this.grunnlagSammenlikningsresultat = grunnlagSammenlikningsresultat;
        this.perioderMedEndringIEtablertTilsyn = perioderMedEndringIEtablertTilsyn;
    }

    @Override
    public Map<Node, Pair> getLeafDifferences() {
        if (areDifferent()) {
            return new DiffEntity(TraverseEntityGraphFactory.build())
                .diff(null, grunnlagSammenlikningsresultat)
                .getLeafDifferences();
        }
        return Map.of();
    }

    @Override
    public boolean isEmpty() {
        var sykdomEmpty = grunnlagSammenlikningsresultat == null || grunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
        var tilsynEmpty = perioderMedEndringIEtablertTilsyn == null || perioderMedEndringIEtablertTilsyn.isEmpty();
        return sykdomEmpty && tilsynEmpty;
    }

    @Override
    public boolean areDifferent() {
        var sykdomEndring = grunnlagSammenlikningsresultat != null && !grunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
        var tilsynEndring = perioderMedEndringIEtablertTilsyn != null && !perioderMedEndringIEtablertTilsyn.isEmpty();
        return sykdomEndring || tilsynEndring;
    }

    @Override
    public String toString() {
        return "SykdomDiffResult{" +
            "isEmpty=" + isEmpty() +
            ", areDifferent=" + areDifferent() +
            ", sykdomDiff=" + (grunnlagSammenlikningsresultat != null ? grunnlagSammenlikningsresultat.getDiffPerioder().toString() : "[]") +
            ", tilsynDiff=" + (perioderMedEndringIEtablertTilsyn != null ? perioderMedEndringIEtablertTilsyn.toString() : "[]") +
            "}";
    }
}
