package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

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
        var ytelseType = fagsak.getYtelseType();
        var saksnummer = fagsak.getSaksnummer();
        var dumpsters = findDumpsters(ytelseType);
        List<DumpOutput> allDumps = dumpOutput(fagsak, dumpsters);

        return new ZipOutput().dump(saksnummer, allDumps);
    }

    private List<Instance<DebugDumpFagsak>> findDumpsters(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.list(DebugDumpFagsak.class, dumpere, ytelseType.getKode());
    }

    private List<DumpOutput> dumpOutput(Fagsak fagsak, List<Instance<DebugDumpFagsak>> dumpsters) {
        var dumpers = dumpsters.stream().flatMap(v -> v.stream()).collect(Collectors.toList());
        var dumperNames = dumpers.stream().map(d -> d.getClass().getName()).collect(Collectors.toList());
        log.info("Dumper fra: {}", dumperNames);

        List<DumpOutput> allDumps = dumpers.stream().flatMap(ddp -> {
            try {
                return ddp.dump(fagsak).stream();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                return Stream.of(new DumpOutput(ddp.getClass().getSimpleName() + "-ERROR.txt", sw.toString()));
            }
        }).collect(Collectors.toList());
        return allDumps;
    }

}
