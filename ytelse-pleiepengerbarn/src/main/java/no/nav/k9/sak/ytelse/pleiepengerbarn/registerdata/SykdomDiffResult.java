package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.Map;

import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.behandlingslager.diff.Node;
import no.nav.k9.sak.behandlingslager.diff.Pair;
import no.nav.k9.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraph;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;

public class SykdomDiffResult extends DiffResult {

    private SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat;

    public SykdomDiffResult(TraverseGraph traverser, TraverseGraph.TraverseResult result1, TraverseGraph.TraverseResult result2) {
        super(traverser, result1, result2);
    }

    public SykdomDiffResult(SykdomGrunnlagSammenlikningsresultat grunnlagSammenlikningsresultat) {
        this(null, null, null);
        this.grunnlagSammenlikningsresultat = grunnlagSammenlikningsresultat;
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
        return grunnlagSammenlikningsresultat == null || grunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
    }

    @Override
    public boolean areDifferent() {
        return grunnlagSammenlikningsresultat != null && !grunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
    }

    @Override
    public String toString() {
        return "SykdomDiffResult{" +
            "isEmpty=" + isEmpty() +
            ", areDifferent=" + areDifferent() +
            ", diffPerioder=" + (grunnlagSammenlikningsresultat != null ? grunnlagSammenlikningsresultat.getDiffPerioder().toString() : "[]") +
            "}";
    }
}
