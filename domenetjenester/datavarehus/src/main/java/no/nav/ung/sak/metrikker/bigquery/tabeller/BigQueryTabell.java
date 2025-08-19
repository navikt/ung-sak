package no.nav.ung.sak.metrikker.bigquery.tabeller;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.Schema;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;

import java.util.Map;
import java.util.function.Function;

/**
 * En type‐safe definisjon for one BigQuery tabell:
 * tabellnavn: navnet på tabellen i BigQuery
 * skjema: Skjemadefinisjon for tabellen, som beskriver kolonnene og deres typer
 * recordClass: klassen som representerer en rad i tabellen (POJO)
 * mapper: funksjon som konverterer en instans av recordClass til en Map<String, Object>
 */
public class BigQueryTabell<T extends BigQueryRecord> {
    private final String tabellnavn;
    private final Schema skjema;
    private final Class<T> dataKlasse;
    private final Function<T, Map<String, Object>> mapperFunksjon;

    public BigQueryTabell(
        String tabellnavn,
        Schema skjema,
        Class<T> dataKlasse,
        Function<T, Map<String, Object>> mapperFunksjon
    ) {
        this.tabellnavn = tabellnavn;
        this.skjema = skjema;
        this.dataKlasse = dataKlasse;
        this.mapperFunksjon = mapperFunksjon;
    }

    public String getTabellnavn() {
        return tabellnavn;
    }

    public Schema getSkjema() {
        return skjema;
    }

    public Class<T> getDataKlasse() {
        return dataKlasse;
    }

    /**
     * Cast-check + Kall mapperFunksjon for å konvertere en instans av dataKlasse til en Map<String, Object>.
     *
     * @param data instansen av dataKlasse som skal konverteres
     */
    public InsertAllRequest.RowToInsert tilRowInsert(T data) {
        if (!dataKlasse.isInstance(data)) {
            throw new IllegalArgumentException(
                "Forventet " + dataKlasse.getSimpleName() +
                    " men fikk " + data.getClass().getSimpleName()
            );
        }
        return InsertAllRequest.RowToInsert.of(mapperFunksjon.apply(data));
    }
}
