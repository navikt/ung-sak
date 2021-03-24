package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.List;

import no.nav.k9.sak.typer.Saksnummer;

public interface DebugDumpFagsak {

    List<DumpOutput> dump(Saksnummer saksnummer);
}
