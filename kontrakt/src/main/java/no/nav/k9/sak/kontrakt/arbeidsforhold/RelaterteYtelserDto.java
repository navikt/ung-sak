package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RelaterteYtelserDto {

    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @JsonProperty(value = "relatertYtelseType")
    private String relatertYtelseType;

    @Size(max = 100)
    @Valid
    @JsonProperty(value = "tilgrensendeYtelserListe")
    private List<TilgrensendeYtelserDto> tilgrensendeYtelserListe = new ArrayList<>();

    public RelaterteYtelserDto(String relatertYtelseType, List<TilgrensendeYtelserDto> tilgrensendeYtelserListe) {
        this.relatertYtelseType = relatertYtelseType;
        if (tilgrensendeYtelserListe != null) {
            this.tilgrensendeYtelserListe.addAll(tilgrensendeYtelserListe);
        }
    }

    public String getRelatertYtelseType() {
        return relatertYtelseType;
    }

    public List<TilgrensendeYtelserDto> getTilgrensendeYtelserListe() {
        return tilgrensendeYtelserListe;
    }
}
