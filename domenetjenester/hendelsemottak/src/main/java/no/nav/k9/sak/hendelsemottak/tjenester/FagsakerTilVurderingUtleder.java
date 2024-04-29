package no.nav.k9.sak.hendelsemottak.tjenester;


import java.util.Map;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;

public interface FagsakerTilVurderingUtleder {

    Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse);
}
