package no.nav.k9.sak.kompletthet;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;

@ApplicationScoped
public class KompletthetsjekkerProvider {

    public Kompletthetsjekker finnKompletthetsjekkerFor(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, ytelseType, behandlingType).get();
    }
}
