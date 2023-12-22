package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

public interface DebugDumpFagsak {

    List<DumpOutput> dump(Fagsak fagsak);

    default void dump(DumpMottaker dumpMottaker) {
        List<DumpOutput> dumpOutputs = dump(dumpMottaker.getFagsak());
        for (DumpOutput dumpOutput : dumpOutputs) {
            try {
                dumpMottaker.newFile(dumpOutput.getPath());
                dumpMottaker.getOutputStream().write(dumpOutput.getContent().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
