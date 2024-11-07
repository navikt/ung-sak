package no.nav.k9.sak.kontrakt.søknad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OppgittTilknytningDto {

    @JsonProperty(value = "utlandsopphold")
    @Valid
    @Size(max = 50)
    private List<UtlandsoppholdDto> utlandsopphold = new ArrayList<>();

    public OppgittTilknytningDto() {
        // trengs for deserialisering av JSON
    }

    public OppgittTilknytningDto(List<UtlandsoppholdDto> utlandsopphold) {
        this.utlandsopphold = List.copyOf(utlandsopphold);
    }

    public List<UtlandsoppholdDto> getUtlandsopphold() {
        return Collections.unmodifiableList(utlandsopphold);
    }
}
