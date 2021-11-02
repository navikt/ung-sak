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

/**
 * Grunnlagdiff som alltid gir diff. Dette er OK siden StartpunktUtlederPleiepengerSyktBarn
 * kjører diff selv.
 */
public class PSBGrunnlagDiff extends DiffResult {

    public PSBGrunnlagDiff() {
        super(null, null, null);
    }

    @Override
    public Map<Node, Pair> getLeafDifferences() {
        if (areDifferent()) {
            return new DiffEntity(TraverseEntityGraphFactory.build())
                .diff(null, new PSBDiffData())
                .getLeafDifferences();
        }
        return Map.of();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean areDifferent() {
        return true;
    }

    @Override
    public String toString() {
        return "SykdomDiffResult{" +
            "isEmpty=" + isEmpty() +
            ", areDifferent=" + areDifferent() +
            "}";
    }
}
