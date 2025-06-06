package no.nav.ung.sak.web.app.tjenester.forvaltning;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.Tuple;
import no.nav.ung.kodeverk.api.Kodeverdi;

public class CsvOutput {

    private static final char UTF8_BOM = '\ufeff';

    public static <V> String dumpAsCsv(boolean includeHeader, List<V> input, Map<String, Function<V, ?>> valueMapper) {
        var sb = new StringBuilder(500);
        sb.append(UTF8_BOM);
        if (includeHeader) {
            sb.append(csvHeader(valueMapper));
        }
        for (var v : input) {
            sb.append(csvValueRow(v, valueMapper)).append('\n');
        }
        return sb.toString();
    }

    public static <V> String dumpAsCsvSingleInput(boolean includeHeader, V input, Map<String, Function<V, ?>> valueMapper) {
        var sb = new StringBuilder(500);
        sb.append(UTF8_BOM);
        if (includeHeader) {
            sb.append(csvHeader(valueMapper));
        }
        sb.append(csvValueRow(input, valueMapper));
        return sb.toString();
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

    public static Optional<String> dumpResultSetToCsv(List<Tuple> results) {
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

        return Optional.of(dumpAsCsv(true, results, toCsv));
    }

    /** Muliggjør mer effektiv bruk av minne enn å ta en Liste av tuple. */
    public static Optional<String> dumpResultSetToCsv(Stream<Tuple> results) {

        class CsvCollector implements Consumer<Tuple> {
            private Tuple firstRow;
            private Map<String, Function<Tuple, ?>> toCsv = new LinkedHashMap<>();
            private StringBuilder sb = new StringBuilder(100000);

            @Override
            public void accept(Tuple t) {
                if (firstRow == null) {
                    // header row
                    firstRow = t;
                    int col = 0;
                    for (var c : firstRow.getElements()) {
                        int thisCol = col++;
                        toCsv.put(Optional.ofNullable(c.getAlias()).orElse("col-" + thisCol), r -> r.get(thisCol));
                    }
                    sb.append(csvHeader(toCsv));
                } else {
                    // data row
                    sb.append(csvValueRow(t, toCsv)).append('\n');
                }
            }
        }

        var collector = new CsvCollector();
        results.forEach(collector);

        if (collector.firstRow == null) {
            return Optional.empty();
        } else {
            return Optional.of(UTF8_BOM + collector.sb.toString());
        }
    }

    public static Optional<DumpOutput> dumpResultSetToCsv(String path, Stream<Tuple> results) {

        class CsvCollector implements Consumer<Tuple> {
            private Tuple firstRow;
            private Map<String, Function<Tuple, ?>> toCsv = new LinkedHashMap<>();
            private StringBuilder sb = new StringBuilder(100000);

            @Override
            public void accept(Tuple t) {
                if (firstRow == null) {
                    // header row
                    firstRow = t;
                    int col = 0;
                    for (var c : firstRow.getElements()) {
                        int thisCol = col++;
                        toCsv.put(Optional.ofNullable(c.getAlias()).orElse("col-" + thisCol), r -> r.get(thisCol));
                    }
                    sb.append(csvHeader(toCsv));
                } else {
                    // data row
                    sb.append(csvValueRow(t, toCsv)).append('\n');
                }
            }
        }

        var collector = new CsvCollector();
        results.forEach(collector);

        if (collector.firstRow == null) {
            return Optional.empty();
        } else {
            return Optional.of(new DumpOutput(path, UTF8_BOM + collector.sb.toString()));
        }
    }

}
