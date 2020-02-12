package no.nav.k9.sak.kontrakt.dokument;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.k9.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.k9.sak.typer.JournalpostId;

public class DokumentDto {
    private List<Long> behandlinger = new ArrayList<>();
    private String dokumentId;
    private String gjelderFor;
    private JournalpostId journalpostId;
    private Kommunikasjonsretning kommunikasjonsretning;
    private LocalDateTime tidspunkt;
    private String tittel;

    public DokumentDto() {
        this.behandlinger = new ArrayList<>();
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

    public String getDokumentId() {
        return dokumentId;
    }

    public String getGjelderFor() {
        return gjelderFor;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Kommunikasjonsretning getKommunikasjonsretning() {
        return kommunikasjonsretning;
    }

    public LocalDateTime getTidspunkt() {
        return tidspunkt;
    }

    public String getTittel() {
        return tittel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, dokumentId, behandlinger, tidspunkt, tidspunkt, tittel, kommunikasjonsretning, gjelderFor);
    }

    public void setBehandlinger(List<Long> behandlinger) {
        this.behandlinger = List.copyOf(behandlinger);
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public void setGjelderFor(String gjelderFor) {
        this.gjelderFor = gjelderFor;
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    public void setKommunikasjonsretning(Kommunikasjonsretning kommunikasjonsretning) {
        this.kommunikasjonsretning = kommunikasjonsretning;
    }

    public void setTidspunkt(LocalDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }
}
