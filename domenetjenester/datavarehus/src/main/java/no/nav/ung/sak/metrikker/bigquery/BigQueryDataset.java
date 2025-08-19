package no.nav.ung.sak.metrikker.bigquery;

/**
 * Sørg for at datasetName samsvarer med bigQueryDatasets.name i NAIS config.
 */
public enum BigQueryDataset {
    UNG_SAK_STATISTIKK_DATASET("ung_sak_statistikk_dataset");

    private final String datasetNavn;

    BigQueryDataset(String datasetNavn) {
        this.datasetNavn = datasetNavn;
    }

    public String getDatasetNavn() {
        return datasetNavn;
    }
}
