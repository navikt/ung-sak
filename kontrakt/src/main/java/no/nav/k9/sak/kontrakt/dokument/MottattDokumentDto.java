package no.nav.k9.sak.kontrakt.dokument;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class MottattDokumentDto {

    @JsonProperty(value = "mottattDato", required = true)
    @NotNull
    private LocalDate mottattDato;

    @JsonProperty(value = "dokumentTypeId", required = true)
    @NotNull
    private DokumentTypeId dokumentTypeId;

    @JsonProperty(value = "dokumentKategori", required = true)
    @NotNull
    private DokumentKategori dokumentKategori;

    public MottattDokumentDto() {
        //
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public void setDokumentTypeId(DokumentTypeId dokumentTypeId) {
        this.dokumentTypeId = dokumentTypeId;
    }

    public void setDokumentKategori(DokumentKategori dokumentKategori) {
        this.dokumentKategori = dokumentKategori;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public DokumentTypeId getDokumentTypeId() {
        return dokumentTypeId;
    }

    public DokumentKategori getDokumentKategori() {
        return dokumentKategori;
    }
}
