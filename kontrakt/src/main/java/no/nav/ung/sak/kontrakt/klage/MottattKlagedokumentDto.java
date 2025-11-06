package no.nav.ung.sak.kontrakt.klage;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MottattKlagedokumentDto {

    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @JsonProperty(value = "dokumentTypeId")
    @Valid
    @Size(max = 8)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*")
    private String dokumentTypeId;

    @JsonProperty(value = "dokumentKategori")
    @Valid
    @Size(max = 8)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*")
    private String dokumentKategori;

    @JsonProperty(value = "behandlingId")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @JsonProperty(value = "mottattDato")
    private LocalDate mottattDato;

    @JsonProperty(value = "mottattTidspunkt")
    private LocalDateTime mottattTidspunkt;

    @Size(max = 3000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String xmlPayload;

    @NotNull
    @JsonProperty("elektroniskRegistrert")
    private boolean elektroniskRegistrert;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long fagsakId;

    public MottattKlagedokumentDto() {
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public void setDokumentTypeId(String dokumentTypeId) {
        this.dokumentTypeId = dokumentTypeId;
    }

    public void setDokumentKategori(String dokumentKategori) {
        this.dokumentKategori = dokumentKategori;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public void setMottattTidspunkt(LocalDateTime mottattTidspunkt) {
        this.mottattTidspunkt = mottattTidspunkt;
    }

    public void setXmlPayload(String xmlPayload) {
        this.xmlPayload = xmlPayload;
    }

    public void setElektroniskRegistrert(boolean elektroniskRegistrert) {
        this.elektroniskRegistrert = elektroniskRegistrert;
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentTypeId() {
        return dokumentTypeId;
    }

    public String getDokumentKategori() {
        return dokumentKategori;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public String getXmlPayload() {
        return xmlPayload;
    }

    public boolean isElektroniskRegistrert() {
        return elektroniskRegistrert;
    }

    public Long getFagsakId() {
        return fagsakId;
    }
}
