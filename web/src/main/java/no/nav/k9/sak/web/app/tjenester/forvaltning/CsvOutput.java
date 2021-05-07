package no.nav.k9.sak.web.app.tjenester.forvaltning;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.Tuple;

import no.nav.k9.kodeverk.api.Kodeverdi;

public class CsvOutput {

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
