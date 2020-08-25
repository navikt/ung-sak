package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Set;

import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.k9.kodeverk.behandling.BehandlingType;

public interface InformasjonselementerUtleder {

    public Set<RegisterdataType> utled(BehandlingType behandlingType);

}
