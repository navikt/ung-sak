package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.io.BufferedOutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.StreamingOutput;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
public class DebugDumpsters {

    private static final Logger log = LoggerFactory.getLogger(DebugDumpsters.class);

    private @Any Instance<DebugDumpFagsak> dumpere;

    protected DebugDumpsters() {
        //
    }

    @Inject
    public DebugDumpsters(@Any Instance<DebugDumpFagsak> dumpere) {
        this.dumpere = dumpere;
    }

    public StreamingOutput dumper(Fagsak fagsak) {
        List<Instance<DebugDumpFagsak>> dumpsters = findDumpsters(fagsak.getYtelseType());

        return dumpFraDumpsters(fagsak, dumpsters);
    }

    private List<Instance<DebugDumpFagsak>> findDumpsters(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.list(DebugDumpFagsak.class, dumpere, ytelseType);
    }

    private StreamingOutput dumpFraDumpsters(Fagsak fagsak, List<Instance<DebugDumpFagsak>> dumpsters) {
        List<DebugDumpFagsak> dumpers = dumpsters.stream().flatMap(Instance::stream).toList();
        List<String> dumperNames = dumpers.stream().map(d -> d.getClass().getName()).collect(Collectors.toList());
        log.info("Dumper fra: {}", dumperNames);

        StreamingOutput streamingOutput = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
                final DumpMottaker dumpMottaker = new DumpMottaker(fagsak, zipOut);
                for (DebugDumpFagsak dumper : dumpers) {
                    dump(dumpMottaker, dumper);
                }
                zipOut.closeEntry();
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        };
        return streamingOutput;
    }

    private void dump(DumpMottaker dumpMottaker, DebugDumpFagsak dumpster) {
        log.info("Dumper fra {}", dumpster.getClass().getName());
        try {
            dumpster.dump(dumpMottaker);
        } catch (Exception e) {
            dumpMottaker.newFile(dumpster.getClass().getSimpleName() + "-ERROR.txt");
            dumpMottaker.write(e);
        }
    }
}
