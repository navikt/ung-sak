package no.nav.k9.sak.kontrakt.søknad;

import java.time.LocalDate;
import java.util.Objects;

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

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "landNavn", required = true)
    @NotNull
    @Size(max = 1000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String landNavn;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    public UtlandsoppholdDto() {
        //
    }

    public UtlandsoppholdDto(String landNavn, LocalDate fom, LocalDate tom) {
        this.landNavn = Objects.requireNonNull(landNavn, "landNavn");
        this.fom = Objects.requireNonNull(fom, "fom");
        this.tom = Objects.requireNonNull(tom, "tom");
    }

    public LocalDate getFom() {
        return fom;
    }

    public String getLandNavn() {
        return landNavn;
    }

    public LocalDate getTom() {
        return tom;
    }
}
