package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface DebugDumpBehandling {

    void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath);
}
