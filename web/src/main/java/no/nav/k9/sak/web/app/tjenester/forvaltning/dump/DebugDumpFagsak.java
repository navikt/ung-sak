package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.List;

import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public interface DebugDumpFagsak {

    List<DumpOutput> dump(Fagsak fagsak);
}
