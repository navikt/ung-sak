package no.nav.k9.sak.kontrakt.dokument;

import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.typer.JournalpostId;

public class MottattDokumentDto {
    private Long mottattDokumentId;
    private String dokumentId;
    private String gjelderFor;
    private JournalpostId journalpostId;
    private LocalDateTime tidspunkt;
    private String tittel;
    private String href;
    @JsonIgnore
    private String basePath;
    private DokumentStatus dokumentStatus;

    public MottattDokumentDto(String basePath) {
        this.basePath = basePath + "&journalpostId=%s&dokumentId=%s";
    }

    public Long getMottattDokumentId() {
        return mottattDokumentId;
    }

    public void setMottattDokumentId(Long mottattDokumentId) {
        this.mottattDokumentId = mottattDokumentId;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
        genererLenke();
    }

    public void setGjelderFor(String gjelderFor) {
        this.gjelderFor = gjelderFor;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
        genererLenke();
    }

    public void setTidspunkt(LocalDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    public DokumentStatus getDokumentStatus() {
        return dokumentStatus;
    }

    public void setDokumentStatus(DokumentStatus dokumentStatus) {
        this.dokumentStatus = dokumentStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, dokumentId, tidspunkt, tittel, gjelderFor, dokumentStatus);
    }

    void genererLenke() {
        if (journalpostId != null && journalpostId.getVerdi() != null && dokumentId != null) {
            this.href = String.format(basePath, journalpostId.getVerdi(), dokumentId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MottattDokumentDto that = (MottattDokumentDto) o;
        return Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(dokumentId, that.dokumentId) &&
            Objects.equals(tidspunkt, that.tidspunkt) &&
            Objects.equals(tittel, that.tittel) &&
            Objects.equals(gjelderFor, that.gjelderFor);
    }
}
