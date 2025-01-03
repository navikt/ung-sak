package no.nav.ung.sak.kontrakt.søknad;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonInclude(value = Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class AngittPersonDto {

    @JsonProperty(value = "navn")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonProperty(value = "fødselsdato")
    @Valid
    private LocalDate fødselsdato;

    @JsonProperty(value = "rolle")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String rolle;

    @JsonProperty(value = "situasjonKode")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String situasjonKode;

    @JsonProperty(value = "tilleggsopplysninger")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String tilleggsopplysninger;

    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "personIdent")
    @Valid
    private PersonIdent personIdent;

    public AngittPersonDto setNavn(String navn) {
        this.navn = navn;
        return this;
    }

    public AngittPersonDto setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
        return this;
    }

    public AngittPersonDto setRolle(RelasjonsRolleType rolle) {
        this.rolle = rolle == null ? null : rolle.getKode();
        return this;
    }

    public AngittPersonDto setSituasjonKode(String situasjonKode) {
        this.situasjonKode = situasjonKode;
        return this;
    }

    public AngittPersonDto setTilleggsopplysninger(String tilleggsopplysninger) {
        this.tilleggsopplysninger = tilleggsopplysninger;
        return this;
    }

    public AngittPersonDto setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
        return this;
    }

    public AngittPersonDto setPersonIdent(PersonIdent personIdent) {
        this.personIdent = personIdent;
        return this;
    }

}
