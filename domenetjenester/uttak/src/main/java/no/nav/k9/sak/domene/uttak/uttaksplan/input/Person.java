package no.nav.k9.sak.domene.uttak.uttaksplan.input;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class Person {

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "aktørId")
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "AktørId '${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String aktørId;

    @JsonProperty(value = "fødselsdato", required = true)
    @NotNull
    @PastOrPresent
    private LocalDate fødselsdato;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "dødsdato")
    @PastOrPresent
    private LocalDate dødsdato;

    @JsonCreator
    public Person(@JsonProperty(value = "aktørId") @Size(max = 20) @Pattern(regexp = "^\\d+$", message = "AktørId '${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String aktørId,
                  @JsonProperty(value = "fødselsdato", required = true) @NotNull @PastOrPresent LocalDate fødselsdato,
                  @JsonProperty(value = "dødsdato") @PastOrPresent LocalDate dødsdato) {
        this.aktørId = aktørId;
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
    }

    public String getAktørId() {
        return aktørId;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

}
