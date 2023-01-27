package no.nav.k9.sak.kontrakt.opplæringspenger.dokument;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.ResourceLink;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OpplæringDokumentDto {

    @JsonProperty(value = "id")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String id;

    @JsonProperty(value = "type")
    @Valid
    private OpplæringDokumentType type;

    @JsonProperty(value = "datert")
    @Valid
    private LocalDate datert;

    @JsonProperty(value = "fremhevet")
    @Valid
    private boolean fremhevet;

    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    public OpplæringDokumentDto() {
    }

    public OpplæringDokumentDto(String id, OpplæringDokumentType type, LocalDate datert, boolean fremhevet, List<ResourceLink> links) {
        this.id = id;
        this.type = type;
        this.datert = datert;
        this.fremhevet = fremhevet;
        this.links = links;
    }

    public String getId() {
        return id;
    }
}
