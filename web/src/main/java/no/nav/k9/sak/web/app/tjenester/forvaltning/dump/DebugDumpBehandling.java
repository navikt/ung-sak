package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.List;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface DebugDumpBehandling {

    List<DumpOutput> dump(Behandling behandling);
}
