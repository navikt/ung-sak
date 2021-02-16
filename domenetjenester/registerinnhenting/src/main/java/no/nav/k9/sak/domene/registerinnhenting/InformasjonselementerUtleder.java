package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Set;

import javax.enterprise.inject.Instance;

import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;

public interface InformasjonselementerUtleder {

    public Set<RegisterdataType> utled(BehandlingType behandlingType);

    public static InformasjonselementerUtleder finnTjeneste(Instance<InformasjonselementerUtleder> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(InformasjonselementerUtleder.class, instances, ytelseType, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke utleder for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }
}
