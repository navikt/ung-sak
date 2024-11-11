package no.nav.ung.sak.kontrakt.fagsak;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class RelatertSakDto {

    @JsonInclude(value = Include.NON_ABSENT)
    @JsonProperty(value = "relaterteSøkere", required = false)
    @Size(max = 20)
    @Valid
    private List<RelatertSøkerDto> relaterteSøkere;


    protected RelatertSakDto() {
        //
    }

    public RelatertSakDto(
            @Valid @NotNull List<RelatertSøkerDto> relaterteSøkere) {
        this.relaterteSøkere = relaterteSøkere;
    }
}
