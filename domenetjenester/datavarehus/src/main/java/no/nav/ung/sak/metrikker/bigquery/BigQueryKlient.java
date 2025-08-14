package no.nav.ung.sak.metrikker.bigquery;

import com.google.cloud.bigquery.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;

import java.util.Arrays;
import java.util.Collection;
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

    private final BigQuery bigQuery;

    public BigQueryKlient() {
        // For CDI proxying
        this.bigQuery = null;
    }

    /**
     * Konstruktør for BigQueryKlient som sørger for at nødvendige BigQuery-datasett eksisterer ved oppstart.
     *
     * @param bigQueryEnabled Angir om BigQuery er aktivert.
     * @param bigQuery        Instans av BigQuery som injiseres via CDI fra BigQueryProducer.
     */
    @Inject
    public BigQueryKlient(@KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled, BigQuery bigQuery) {
        this.bigQuery = bigQuery;
        if (bigQueryEnabled && bigQuery != null) {
            Arrays.stream(BIG_QUERY_DATASET).forEach(this::forsikreDatasetEksisterer);
        }
    }

    /**
     * Publiserer en data med data til en spesifikk BigQuery-tabell.
     *
     * @param dataset  Datasettet som tabellen tilhører.
     * @param tableDef Tabellen som data skal publiseres til. Dersom tabellen ikke finnes, vil den bli opprettet.
     * @param records  Dataene som skal publiseres. Dette er en liste av objekter som implementerer BigQueryRecord.
     */
    public <T extends BigQueryRecord> void publish(
        BigQueryDataset dataset,
        BigQueryTabell<T> tableDef,
        Collection<T> records
    ) {

        if (bigQuery == null) {
            throw new IllegalStateException("Utviklerfeil: BigQuery er ikke instansiert. {}");
        }

        // 1) map
        if (records == null || records.isEmpty()) {
            log.warn("Ingen data å publisere til BigQuery-tabell {}", tableDef.getTabellnavn());
            return;
        }
        List<InsertAllRequest.RowToInsert> rader = records.stream().map(tableDef::tilRowInsert).toList();

        TableId tableId = hentEllerOpprettTabell(dataset.getDatasetNavn(), tableDef);

        InsertAllRequest req = InsertAllRequest.newBuilder(tableId)
            .setRows(rader)
            .build();

        InsertAllResponse insertAllResponse = bigQuery.insertAll(req);
        håndterResponse(insertAllResponse, req.getRows().size(), tableDef.getTabellnavn());
    }

    /**
     * Henter TableId for en BigQuery-tabell.
     * Hvis tabellen allerede finnes, brukes den eksisterende tabellen.
     * Hvis tabellen ikke finnes, opprettes en ny tabell med det angitte navnet og skjemaet.
     *
     * @param datasetNavn Navnet på BigQuery-datasettet tabellen tilhører.
     * @param tableDef    Eksisterende BigQuery-tabell, kan være null hvis tabellen ikke finnes.
     * @return TableId for den eksisterende eller nye tabellen.
     */
    private TableId hentEllerOpprettTabell(String datasetNavn, BigQueryTabell<?> tableDef) {
        Table existing = bigQuery.getTable(TableId.of(datasetNavn, tableDef.getTabellnavn()));
        if (existing != null) {
            log.info("Bruker eksisterende tabell {}", existing.getTableId());
            return existing.getTableId();
        }

        // Opprett
        TableId tableId = TableId.of(datasetNavn, tableDef.getTabellnavn());
        TableDefinition tableDefinition = StandardTableDefinition.newBuilder()
            .setSchema(tableDef.getSkjema())
            .build();

        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
        bigQuery.create(tableInfo);
        log.info("Opprettet nytt tabell {}", tableId);
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
                log.error("Dataset {} eksisterer ikke i BigQuery. Opprett en dataset i BigQuery før du publiserer data.", datasetNavn);
                throw new RuntimeException("Dataset " + datasetNavn + " eksisterer ikke i BigQuery. Opprett dataset før publisering.");
            }
        } catch (BigQueryException e) {
            log.error("Noe gikk galt ved forsøk på å hente dataset {}: {}", datasetNavn, e.getMessage(), e);
            throw new RuntimeException("Kunne ikke hente dataset " + datasetNavn + " fra BigQuery.", e);
        }
    }

    /**
     * Håndterer responsen fra BigQuery etter et forsøk på å sette inn data.
     * Hvis det er feil i innsettingen, logges feilmeldingene og en RuntimeException kastes.
     *
     * @param response    Responsen fra BigQuery etter innsetting av data.
     * @param antallRader Antall rader som ble forsøkt satt inn.
     * @param tabellnavn Navnet på tabellen som data ble forsøkt satt inn i.
     */
    private static void håndterResponse(InsertAllResponse response, int antallRader, String tabellnavn) {
        if (response.hasErrors()) {
            response.getInsertErrors()
                .forEach((idx, errs) -> {
                    errs.forEach(err -> log.error("BigQuery insert feilet for rad {}: {}", idx, err));
                });
            throw new RuntimeException("BigQuery insert feilet for noen rader: " + response.getInsertErrors().size());
        } else log.info("BigQuery skrev {} rader inn i {}.", antallRader, tabellnavn);
    }

    /**
     * Sletter all data fra en spesifisert BigQuery-tabell.
     *
     * @param dataset  Datasettet som tabellen tilhører.
     * @param tableDef Tabellen som skal tømmes.
     */
    public <T extends BigQueryRecord> void slettAllData(BigQueryDataset dataset, BigQueryTabell<T> tableDef) {
        if (bigQuery == null) {
            throw new IllegalStateException("Utviklerfeil: BigQuery er ikke instansiert. {} ");
        }
        if (!tableDef.skalEksisterendeInnholdSlettesFørPublisering()) {
            throw new IllegalArgumentException("Kan ikke slette data fra BigQuery-tabell " + tableDef.getTabellnavn() + " fordi skalEksisterendeInnholdSlettesFørPublisering er satt til false.");
        }
        Table existing = bigQuery.getTable(TableId.of(dataset.getDatasetNavn(), tableDef.getTabellnavn()));
        if (existing == null) {
            log.info("Fant ikke tabell med navn {}, utfører ikke sletting", tableDef.getTabellnavn());
            return;
        }

        String query = String.format("DELETE FROM `%s.%s` WHERE TRUE", dataset.getDatasetNavn(), tableDef.getTabellnavn());
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
        try {
            bigQuery.query(queryConfig);
            log.info("Slettet all data fra BigQuery-tabell {}.{}", dataset.getDatasetNavn(), tableDef.getTabellnavn());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sletting av data fra BigQuery-tabell {}.{} ble avbrutt", dataset.getDatasetNavn(), tableDef.getTabellnavn(), e);
            throw new RuntimeException("Sletting av data fra BigQuery-tabell ble avbrutt", e);
        } catch (BigQueryException e) {
            log.error("Feil ved sletting av data fra BigQuery-tabell {}.{}: {}", dataset.getDatasetNavn(), tableDef.getTabellnavn(), e.getMessage(), e);
            throw new RuntimeException("Feil ved sletting av data fra BigQuery-tabell", e);
        }
    }
}
