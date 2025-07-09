package no.nav.ung.sak.metrikker.bigquery;

public enum BigQueryTable {
    FAGSAK_STATUS_TABELL_V1("fagsak_status_v1");

    private final String tableNavn;

    BigQueryTable(String tableNavn) {
        this.tableNavn = tableNavn;
    }

    public String getTableNavn() {
        return tableNavn;
    }
}
