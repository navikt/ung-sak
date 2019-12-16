package no.nav.foreldrepenger.kompletthet;

import javax.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class KompletthetsjekkerProvider {

    public Kompletthetsjekker finnKompletthetsjekkerFor(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, ytelseType, behandlingType).get();
    }
}
