package no.nav.k9.sak.kontrakt.vedtak;

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
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DokumentMedUstrukturerteDataDto {

    @JsonProperty(value = "id", required = true)
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String id;

    @JsonProperty(value = "type", required = true)
    @Valid
    private SykdomDokumentType type;

    @JsonProperty(value = "datert")
    @Valid
    private LocalDate datert;

    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    public DokumentMedUstrukturerteDataDto(
            String id,
            SykdomDokumentType type,
            LocalDate datert,
            List<ResourceLink> links) {
        this.id = id;
        this.type = type;
        this.datert = datert;
        this.links = links;
    }
}
