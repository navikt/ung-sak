package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.StreamingOutput;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class DebugDumpsters {

    public StreamingOutput dumper(FagsakYtelseType ytelseType, Saksnummer saksnummer) {
        StreamingOutput streamingOutput = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));) {
                var dumpsters = FagsakYtelseTypeRef.Lookup.list(DebugDumpFagsak.class, CDI.current().select(DebugDumpFagsak.class), ytelseType.getKode());
                for (var inst : dumpsters) {
                    for (var ddp : inst) {
                        var dumps = ddp.dump(ytelseType, saksnummer);
                        for (var dump : dumps) {
                            ZipEntry zipEntry = new ZipEntry(saksnummer + "/" + dump.getPath());
                            try {
                                zipOut.putNextEntry(zipEntry);
                                zipOut.write(dump.getContent().getBytes(Charset.forName("UTF8")));
                                zipOut.closeEntry();
                            } catch (IOException e) {
                                throw new IllegalStateException("Kunne ikke zippe dump fra : " + ddp);
                            }
                        }
                    }
                }

            } finally {
                outputStream.flush();
                outputStream.close();
            }
        };

        return streamingOutput;

    }

    static <V> DumpOutput dumpAsCsv(boolean includeHeader, V input, String path, StringBuilder sb, Map<String, Function<V, ?>> valueMapper) {

        if (includeHeader) {
            var headers = valueMapper.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(","));
            sb.append(headers);
            sb.append("\n");
        }

        var values = valueMapper.values().stream().map(v -> {
            var s = v.apply(input);
            var obj = transformValue(s);
            return s == null || "null".equals(s) ? "" : "\"" + String.valueOf(obj).replace("\"", "\"\"") + "\""; // csv escape and quoting
        }).collect(Collectors.joining(","));

        sb.append(values);
        return new DumpOutput(path, sb.toString());
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
}
