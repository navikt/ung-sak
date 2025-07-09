package no.nav.ung.sak.metrikker.bigquery;

import com.google.cloud.bigquery.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

import java.util.Arrays;
import java.util.List;
/**
 * Klient for å håndtere interaksjoner med Google BigQuery.
 * Sørger for at nødvendige datasett eksisterer og håndterer innsetting av data i BigQuery-tabeller.
 * Denne klassen er ansvarlig for å opprette tabeller hvis de ikke finnes,
 * og for å forsikre at datasett eksisterer før data publiseres.
 * <p>
 * Data publiseres som JSON i et felt kalt "jsonData" i tabellene.
 */
@ApplicationScoped
public class BigQueryKlient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BigQueryKlient.class);

    private static final BigQueryDataset[] BIG_QUERY_DATASET = BigQueryDataset.values();

    private static final Field JSON_DATA_SCHEMA_FIELD = Field.of("jsonData", StandardSQLTypeName.JSON);
    private static final Field TIMESTAMP_SCHEMA_FIELD = Field.of("timestamp", StandardSQLTypeName.DATETIME);

    private final BigQuery bigQuery;

    public BigQueryKlient() {
        // For CDI proxying
        this.bigQuery = null;
    }

    /**
     * Konstruktør for BigQueryKlient som initialiserer BigQuery-tjenesten.
     * Forsikrer at nødvendige BigQuery-datasett eksisterer ved oppstart.
     */
    @Inject
    public BigQueryKlient(@KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled) {
        if(bigQueryEnabled) {
            this.bigQuery = BigQueryOptions.getDefaultInstance().getService();
            Arrays.stream(BIG_QUERY_DATASET).forEach(this::forsikreDatasetEksisterer);
        } else this.bigQuery = null;
    }

    /**
     * Publiserer en rad med data til en spesifikk BigQuery-tabell.
     *
     * @param bigQueryDataset Datasettet som tabellen tilhører.
     * @param bigQueryTable   Tabellen som data skal publiseres til. Dersom tabellen ikke finnes, vil den bli opprettet.
     * @param rad             Dataen som skal publiseres, representert som en rad i BigQuery. Verdien av "jsonData" feltet må være en gyldig JSON-streng.
     */
    public void publish(BigQueryDataset bigQueryDataset, BigQueryTable bigQueryTable, InsertAllRequest.RowToInsert rad) {
        String datasetNavn = bigQueryDataset.getDatasetNavn();
        String tableNavn = bigQueryTable.getTableNavn();
        Table eksisterendeTable = bigQuery.getTable(TableId.of(datasetNavn, tableNavn));

        TableId tableId = hentTableId(eksisterendeTable, datasetNavn, tableNavn);

        InsertAllResponse response = bigQuery.insertAll(
            InsertAllRequest.newBuilder(tableId)
                .setRows(List.of(rad))
                .build());

        håndterResponse(response);
    }

    /**
     * Håndterer responsen fra BigQuery etter et forsøk på å sette inn data.
     * Hvis det er feil i innsettingen, logges feilmeldingene og en RuntimeException kastes.
     * @param response Responsen fra BigQuery etter innsetting av data.
     */
    private static void håndterResponse(InsertAllResponse response) {
        if (response.hasErrors()) {
            response.getInsertErrors()
                .forEach((idx, errs) -> {
                    errs.forEach(err -> log.error("BigQuery insert feilet for rad {}: {}", idx, err.getMessage()));
                });
            throw new RuntimeException("BigQuery insert feilet for noen rader: " + response.getInsertErrors().size());
        }
    }

    /**
     * Henter TableId for en BigQuery-tabell.
     * Hvis tabellen allerede finnes, brukes den eksisterende tabellen.
     * Hvis tabellen ikke finnes, opprettes en ny tabell med det angitte navnet og skjemaet.
     *
     * @param eksisterendeTable Eksisterende BigQuery-tabell, kan være null hvis tabellen ikke finnes.
     * @param datasetNavn       Navnet på BigQuery-datasettet tabellen tilhører.
     * @param tableNavn         Navnet på BigQuery-tabellen som skal opprettes eller brukes.
     * @return TableId for den eksisterende eller nye tabellen.
     */
    private TableId hentTableId(Table eksisterendeTable, String datasetNavn, String tableNavn) {
        TableId tableId;
        if (eksisterendeTable != null) {
            // Hvis tabellen allerede finnes, bruk den eksisterende tabellen
            tableId = eksisterendeTable.getTableId();
            log.info("Bruker eksisterende BigQuery-tabell: {}", tableId);
        } else {
            // Hvis tabellen ikke finnes, opprett en ny tabell
            tableId = TableId.of(datasetNavn, tableNavn);

            Schema schema = Schema.of(JSON_DATA_SCHEMA_FIELD, TIMESTAMP_SCHEMA_FIELD);
            TableDefinition tableDefinition = StandardTableDefinition.newBuilder()
                .setSchema(schema)
                .build();
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
            bigQuery.create(tableInfo);
            log.info("Opprettet ny BigQuery-tabell: {}", tableId);
        }
        return tableId;
    }

    /**
     * Forsikrer at et BigQuery-datasett eksisterer.
     *
     * @param bigQueryDataset Navnet på BigQuery-datasettet som skal sjekkes.
     * @throws RuntimeException hvis datasettet ikke eksisterer.
     */
    private void forsikreDatasetEksisterer(BigQueryDataset bigQueryDataset) {
        String datasetNavn = bigQueryDataset.getDatasetNavn();
        try {
            Dataset dataset = bigQuery.getDataset(DatasetId.of(datasetNavn));
            if (dataset != null) {
                log.info("Forsikret at dataset {} eksisterer i BigQuery.", datasetNavn);
            } else {
                log.error("Dataset {} eksister ikke i BigQuery. Opprett en dataset i BigQuery før du publiserer data.", datasetNavn);
                throw new RuntimeException("Dataset " + datasetNavn + " eksister ikke i BigQuery. Opprett dataset før publisering.");
            }
        } catch (BigQueryException e) {
            log.error("Noe gikk galt ved forsøk på å hente dataset {}: {}", datasetNavn, e.getMessage(), e);
            throw new RuntimeException("Kunne ikke hente dataset " + datasetNavn + " fra BigQuery.", e);
        }
    }
}
