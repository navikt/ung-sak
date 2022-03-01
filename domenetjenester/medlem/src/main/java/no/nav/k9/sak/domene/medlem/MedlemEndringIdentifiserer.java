package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.diff.Node;
import no.nav.k9.sak.behandlingslager.diff.Pair;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class MedlemEndringIdentifiserer {

    public MedlemEndringIdentifiserer() {
    }

    public boolean erEndretFørSkjæringstidspunkt(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, LocalDate skjæringstidspunkt) {
        RegisterdataDiffsjekker differ = new RegisterdataDiffsjekker(true);
        final Map<Node, Pair> nodeEndringer = differ.finnForskjellerPå(grunnlag1 != null ? grunnlag1.getRegistrertMedlemskapPerioder() : null, grunnlag2 != null ? grunnlag2.getRegistrertMedlemskapPerioder() : null);

        return nodeEndringer.keySet()
            .stream()
            .map(Node::getObject)
            .filter(it -> it instanceof MedlemskapPerioderEntitet)
            .anyMatch(adr -> ((MedlemskapPerioderEntitet) adr).getPeriode().getFomDato().isBefore(skjæringstidspunkt));
    }

    public boolean erEndretIPerioden(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, DatoIntervallEntitet periodeTilVurdering) {
        return erEndretIPerioden(grunnlag1, grunnlag2, new TreeSet<>(Set.of(periodeTilVurdering)));
    }

    public boolean erEndretIPerioden(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        RegisterdataDiffsjekker differ = new RegisterdataDiffsjekker(true);
        final Map<Node, Pair> nodeEndringer = differ.finnForskjellerPå(grunnlag1 != null ? grunnlag1.getRegistrertMedlemskapPerioder() : null, grunnlag2 != null ? grunnlag2.getRegistrertMedlemskapPerioder() : null);

        return nodeEndringer.keySet()
            .stream()
            .map(Node::getObject)
            .filter(it -> it instanceof MedlemskapPerioderEntitet)
            .anyMatch(adr -> perioderTilVurdering.stream().anyMatch(it -> it.overlapper(((MedlemskapPerioderEntitet) adr).getPeriode())));
    }
}
