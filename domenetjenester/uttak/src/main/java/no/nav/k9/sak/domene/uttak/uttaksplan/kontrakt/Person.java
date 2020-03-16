package no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
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

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

}
