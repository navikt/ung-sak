package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.List;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

public interface DebugDumpBehandling {

    List<DumpOutput> dump(Behandling behandling);

    default void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        List<DumpOutput> dumpOutputs = dump(behandling);
        for (DumpOutput dumpOutput : dumpOutputs) {
            dumpMottaker.newFile(basePath + "/" + dumpOutput.getPath());
            dumpMottaker.write(dumpOutput.getContent());
        }
    }
}
