package no.nav.ung.sak.formidling.dokdist.dto;

public record DistribuerJournalpostRequest(
    String journalpostId,
    String bestillendeFagsystem,
    String dokumentProdApp,
    DistribusjonsType distribusjonstype,
    String distribusjonstidspunkt
) {

    public enum DistribusjonsType {
        VEDTAK, VIKTIG, ANNET
    }
}
