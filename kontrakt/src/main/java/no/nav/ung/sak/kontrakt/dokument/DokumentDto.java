package no.nav.ung.sak.kontrakt.dokument;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.ung.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.ung.sak.typer.JournalpostId;

public class DokumentDto {
    private List<Long> behandlinger;
    private String dokumentId;
    private String gjelderFor;
    private JournalpostId journalpostId;
    private Kommunikasjonsretning kommunikasjonsretning;
    private LocalDateTime tidspunkt;
    private String tittel;
    private String href;
    @JsonIgnore
    private String basePath;
    private String brevkode;

    public DokumentDto(String basePath) {
        this.basePath = String.format("%s",basePath) + "&journalpostId=%s&dokumentId=%s";
        this.behandlinger = new ArrayList<>();
    }

    public String getHref() {
        return href;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DokumentDto that = (DokumentDto) o;
        return kommunikasjonsretning == that.kommunikasjonsretning &&
            Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(dokumentId, that.dokumentId) &&
            Objects.equals(behandlinger, that.behandlinger) &&
            Objects.equals(tidspunkt, that.tidspunkt) &&
            Objects.equals(tittel, that.tittel) &&
            Objects.equals(gjelderFor, that.gjelderFor);
    }

    public List<Long> getBehandlinger() {
        return Collections.unmodifiableList(behandlinger);
    }

    public void setBehandlinger(List<Long> behandlinger) {
        this.behandlinger = List.copyOf(behandlinger);
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
        genererLenke();
    }

    public void setBrevkode(String brevkode) {
        this.brevkode = brevkode;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public String getGjelderFor() {
        return gjelderFor;
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

    public Kommunikasjonsretning getKommunikasjonsretning() {
        return kommunikasjonsretning;
    }

    public void setKommunikasjonsretning(Kommunikasjonsretning kommunikasjonsretning) {
        this.kommunikasjonsretning = kommunikasjonsretning;
    }

    public LocalDateTime getTidspunkt() {
        return tidspunkt;
    }

    public void setTidspunkt(LocalDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, dokumentId, behandlinger, tidspunkt, tidspunkt, tittel, kommunikasjonsretning, gjelderFor);
    }

    void genererLenke() {
        if (journalpostId != null && journalpostId.getVerdi() != null && dokumentId != null) {
            this.href = String.format(basePath, journalpostId.getVerdi(), dokumentId);
        }

    }
}
