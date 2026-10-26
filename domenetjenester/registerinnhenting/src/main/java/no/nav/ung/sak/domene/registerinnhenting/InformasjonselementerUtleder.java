package no.nav.ung.sak.domene.registerinnhenting;

import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.inject.Instance;

import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;

public interface InformasjonselementerUtleder {

    Set<RegisterdataType> utled(BehandlingType behandlingType);

    Set<RegisterdataType> utled(BehandlingType behandlingType, Collection<BehandlingÅrsakType> behandlingÅrsaker);

    static InformasjonselementerUtleder finnTjeneste(Instance<InformasjonselementerUtleder> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(InformasjonselementerUtleder.class, instances, ytelseType, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke utleder for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

    /** hvorvidt har beregnig/inntekt/arbeid. default basert på om registerdatatype er registrert. */
    default boolean harBeregnetYtelse(BehandlingType type) {
        return !(utled(type).isEmpty());
    }
}
