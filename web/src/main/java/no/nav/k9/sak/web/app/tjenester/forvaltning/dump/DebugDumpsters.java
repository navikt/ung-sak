package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.Tuple;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.Saksnummer;

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
        StreamingOutput streamingOutput = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));) {
                var dumpsters = findDumpsters(ytelseType);
                List<DumpOutput> allDumps = dumpOutput(fagsak, dumpsters);
                allDumps.forEach(dump -> addToZip(saksnummer, zipOut, dump));
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        };

        return streamingOutput;

    }

    private List<Instance<DebugDumpFagsak>> findDumpsters(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.list(DebugDumpFagsak.class, dumpere, ytelseType.getKode());
    }

    private void addToZip(Saksnummer saksnummer, ZipOutputStream zipOut, DumpOutput dump) {
        var zipEntry = new ZipEntry(saksnummer + "/" + dump.getPath());
        try {
            zipOut.putNextEntry(zipEntry);
            zipOut.write(dump.getContent().getBytes(Charset.forName("UTF8")));
            zipOut.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke zippe dump fra : " + dump, e);
        }
    }

    private List<DumpOutput> dumpOutput(Fagsak fagsak, List<Instance<DebugDumpFagsak>> dumpsters) {
        var dumpers = dumpsters.stream().flatMap(v -> v.stream()).collect(Collectors.toList());
        var dumperNames = dumpers.stream().map(d -> d.getClass().getName()).collect(Collectors.toList());
        log.info("Dumper fra: {}", dumperNames);

        List<DumpOutput> allDumps = dumpers.stream().flatMap(ddp -> {
            try {
                return ddp.dump(fagsak).stream();
            } catch (Exception e) {
                log.warn("Kunne ikke dumpe fra : {}", ddp);
                return Stream.empty();
            }
        }).collect(Collectors.toList());
        return allDumps;
    }

    public static <V> DumpOutput dumpAsCsv(boolean includeHeader, List<V> input, String path, Map<String, Function<V, ?>> valueMapper) {
        var sb = new StringBuilder(500);
        if (includeHeader) {
            sb.append(csvHeader(valueMapper));
        }
        for (var v : input) {
            sb.append(csvValueRow(v, valueMapper)).append('\n');
        }
        return new DumpOutput(path, sb.toString());
    }

    public static <V> DumpOutput dumpAsCsvSingleInput(boolean includeHeader, V input, String path, Map<String, Function<V, ?>> valueMapper) {
        var sb = new StringBuilder(500);
        if (includeHeader) {
            sb.append(csvHeader(valueMapper));
        }
        sb.append(csvValueRow(input, valueMapper));
        return new DumpOutput(path, sb.toString());
    }

    private static <V> String csvValueRow(V input, Map<String, Function<V, ?>> valueMapper) {
        var values = valueMapper.values().stream().map(v -> {
            var s = v.apply(input);
            var obj = transformValue(s);
            return s == null || "null".equals(s) ? "" : "\"" + String.valueOf(obj).replace("\"", "\"\"") + "\""; // csv escape and quoting
        }).collect(Collectors.joining(","));
        return values;
    }

    private static <V> String csvHeader(Map<String, Function<V, ?>> valueMapper) {
        var sb = new StringBuilder(200);
        var headers = valueMapper.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(","));
        sb.append(headers);
        sb.append("\n");
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object transformValue(Object in) {
        var out = in instanceof Optional ? ((Optional) in).orElse(null) : in;
        if (out instanceof Kodeverdi) {
            out = ((Kodeverdi) out).getKode();
        }
        if (out instanceof Enum) {
            out = ((Enum) out).name();
        }
        if (out instanceof Collection && ((Collection) out).isEmpty()) {
            out = null;
        }
        if (out instanceof Map && ((Map) out).isEmpty()) {
            out = null;
        }
        return out;
    }

    public static Optional<DumpOutput> dumpResultSetToCsv(String path, List<Tuple> results) {
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }
        var firstRow = results.get(0);

        var toCsv = new LinkedHashMap<String, Function<Tuple, ?>>();

        int col = 0;
        for (var c : firstRow.getElements()) {
            int thisCol = col++;
            toCsv.put(Optional.ofNullable(c.getAlias()).orElse("col-" + thisCol), t -> t.get(thisCol));
        }

        return Optional.of(dumpAsCsv(true, results, path, toCsv));
    }

}
