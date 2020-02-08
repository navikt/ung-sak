package no.nav.k9.sak.kontrakt.søknad;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class UtlandsoppholdDto {

    @JsonProperty(value = "landNavn", required = true)
    @NotNull
    @Size(max = 1000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String landNavn;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    protected UtlandsoppholdDto() {
        //
    }

    public UtlandsoppholdDto(String landNavn, LocalDate fom, LocalDate tom) {
        this.landNavn = landNavn;
        this.fom = fom;
        this.tom = tom;
    }

    public String getLandNavn() {
        return landNavn;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
