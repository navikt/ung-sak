package no.nav.k9.sak.kontrakt.sykdom.dokument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.ResourceLink;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomDokumentOversiktElement {

    
    @JsonProperty(value = "id")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String id;
    
    @JsonProperty(value = "versjon")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String versjon;

    @JsonProperty(value = "type")
    @Valid
    private SykdomDokumentType type;

    @JsonProperty(value = "annenPartErKilde")
    @Valid
    private boolean annenPartErKilde;   
    
    @JsonProperty(value = "datert")
    @Valid
    private LocalDate datert;

    @JsonProperty(value = "mottattDato")
    @Valid
    private LocalDate mottattDato;
    
    @JsonProperty(value = "mottattTidspunkt")
    @Valid
    private LocalDateTime mottattTidspunkt;
    
    @JsonProperty(value = "behandlet")
    @Valid
    private boolean behandlet;
    
    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    
    public SykdomDokumentOversiktElement(
            String id, String versjon, SykdomDokumentType type, boolean annenPartErKilde, LocalDate datert,
            LocalDate mottattDato, LocalDateTime mottattTidspunkt, boolean behandlet,
            List<ResourceLink> links) {
        this.id = id;
        this.versjon = versjon;
        this.type = type;
        this.annenPartErKilde = annenPartErKilde;
        this.datert = datert;
        this.mottattDato = mottattDato;
        this.mottattTidspunkt = mottattTidspunkt;
        this.behandlet = behandlet;
        this.links = links;
    }


    public String getId() {
        return id;
    }
    
    public String getVersjon() {
        return versjon;
    }

    public SykdomDokumentType getType() {
        return type;
    }

    public boolean isAnnenPartErKilde() {
        return annenPartErKilde;
    }

    public LocalDate getDatert() {
        return datert;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public boolean isBehandlet() {
        return behandlet;
    }

    public List<ResourceLink> getLinks() {
        return links;
    }

}
