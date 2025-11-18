package no.nav.ung.sak.hendelsemottak.tjenester;


import java.util.List;
import java.util.Map;

import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

public interface FagsakerTilVurderingUtleder {

    Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse);
}
