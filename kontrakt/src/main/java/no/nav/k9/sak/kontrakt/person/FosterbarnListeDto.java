package no.nav.k9.sak.kontrakt.person;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FosterbarnListeDto {

    @JsonProperty(value = "fosterbarn", required = true)
    @NotNull
    @Valid
    @Size(max = 100)
    private List<FosterbarnDto> fosterbarnDto;

    public FosterbarnListeDto() {
    }

    public FosterbarnListeDto(List<FosterbarnDto> fosterbarnDto) {
        this.fosterbarnDto = fosterbarnDto;
    }

    public List<FosterbarnDto> getFosterbarn() {
        return fosterbarnDto;
    }
}
