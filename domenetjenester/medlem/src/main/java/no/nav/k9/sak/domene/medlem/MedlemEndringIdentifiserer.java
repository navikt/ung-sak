package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.Node;
import no.nav.foreldrepenger.behandlingslager.diff.Pair;

@Dependent
public class MedlemEndringIdentifiserer {

    @Inject
    MedlemEndringIdentifiserer() {
    }

    public boolean erEndretFørSkjæringstidspunkt(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, LocalDate skjæringstidspunkt) {
        RegisterdataDiffsjekker differ = new RegisterdataDiffsjekker(true);
        final Map<Node, Pair> nodeEndringer = differ.finnForskjellerPå(grunnlag1.getRegistrertMedlemskapPerioder(), grunnlag2.getRegistrertMedlemskapPerioder());

        return nodeEndringer.keySet().stream()
            .map(Node::getObject)
            .filter(it -> it instanceof MedlemskapPerioderEntitet)
            .anyMatch(adr -> ((MedlemskapPerioderEntitet) adr).getPeriode().getFomDato().isBefore(skjæringstidspunkt));
    }
}
