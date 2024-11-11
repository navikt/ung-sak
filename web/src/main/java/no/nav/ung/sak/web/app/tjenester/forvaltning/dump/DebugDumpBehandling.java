package no.nav.ung.sak.web.app.tjenester.forvaltning.dump;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface DebugDumpBehandling {

    void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath);
}
