package no.nav.ung.sak.hendelsemottak.tjenester;


import java.util.Map;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

public interface FagsakerTilVurderingUtleder {

    Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse);
}
