package no.nav.k9.sak.domene.medlem.impl;

import java.util.Map;

import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.behandlingslager.diff.Node;
import no.nav.k9.sak.behandlingslager.diff.Pair;
import no.nav.k9.sak.domene.medlem.MedlData;
import no.nav.k9.sak.domene.medlem.diff.MedlDiffsjekker;

public class MedlemEndringssjekker {

    public boolean erEndring(MedlData perioder1, MedlData perioder2) {
        MedlDiffsjekker diffsjekker = new MedlDiffsjekker();
        return erForskjellPå(diffsjekker.getDiffEntity(), perioder1, perioder2);
    }

    private boolean erForskjellPå(DiffEntity diffEntity, Object object1, Object object2) {
        return !finnForskjellerPå(diffEntity, object1, object2).isEmpty();
    }

    private Map<Node, Pair> finnForskjellerPå(DiffEntity diffEntity, Object object1, Object object2) {
        DiffResult diff = diffEntity.diff(object1, object2);
        return diff.getLeafDifferences();
    }
}
