package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import javax.validation.constraints.NotNull;

public class HentDokumentQuery {

    @NotNull
    private final String journalpostId;

    @NotNull
    private final String dokumentId;

    @NotNull
    private final String variantFormat;

    public HentDokumentQuery(String journalpostId, String dokumentId, String variantFormat) {
        this.journalpostId = journalpostId;
        this.dokumentId = dokumentId;
        this.variantFormat = variantFormat;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentInfoId() {
        return dokumentId;
    }

    public String getVariantFormat() {
        return variantFormat;
    }

    @Override
    public String toString() {
        return "HentDokumentQuery{" +
            "journalpostId='" + journalpostId + '\'' +
            ", dokumentId='" + dokumentId + '\'' +
            ", variantFormat='" + variantFormat + '\'' +
            '}';
    }
}
