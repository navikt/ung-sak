package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.typer.PersonIdent;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Barn {

    @JsonProperty(value = "personIdent", required = true)
    @Valid
    @NotNull
    private PersonIdent personIdent;

    @JsonProperty(value = "fødselsdato", required = true)
    @Valid
    @NotNull
    private LocalDate fødselsdato;

    @JsonProperty(value = "dødsdato")
    @Valid
    private LocalDate dødsdato;

    @JsonProperty(value = "harSammeBosted")
    @Valid
    private Boolean harSammeBosted;

    public Barn(@Valid @NotNull PersonIdent personIdent,
                @Valid @NotNull LocalDate fødselsdato, LocalDate dødsdato, @Valid Boolean harSammeBosted) {
        this.personIdent = personIdent;
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
        this.harSammeBosted = harSammeBosted;
    }
}
