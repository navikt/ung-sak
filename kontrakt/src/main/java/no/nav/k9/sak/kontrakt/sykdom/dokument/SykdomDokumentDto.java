package no.nav.k9.sak.kontrakt.sykdom.dokument;

import java.time.LocalDate;
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
public class SykdomDokumentDto {

    @JsonProperty(value = "id")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String id;

    @JsonProperty(value = "type")
    @Valid
    private SykdomDokumentType type;

    @JsonProperty(value = "benyttet")
    @Valid
    private boolean benyttet;

    @JsonProperty(value = "annenPartErKilde")
    @Valid
    private boolean annenPartErKilde;

    @JsonProperty(value = "datert")
    @Valid
    private LocalDate datert;

    @JsonProperty(value = "fremhevet")
    @Valid
    private boolean fremhevet;

    @JsonProperty(value = "harInfoSomIkkeKanPunsjes")
    private boolean harInfoSomIkkeKanPunsjes;

    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    public SykdomDokumentDto(String id, SykdomDokumentType type, boolean benyttet,
                             boolean annenPartErKilde, LocalDate datert, boolean fremhevet,
                             boolean harInfoSomIkkeKanPunsjes, List<ResourceLink> links) {
        this.id = id;
        this.type = type;
        this.benyttet = benyttet;
        this.annenPartErKilde = annenPartErKilde;
        this.datert = datert;
        this.fremhevet = fremhevet;
        this.harInfoSomIkkeKanPunsjes = harInfoSomIkkeKanPunsjes;
        this.links = links;
    }


}
