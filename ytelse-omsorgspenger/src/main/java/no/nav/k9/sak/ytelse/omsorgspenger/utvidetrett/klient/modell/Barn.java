package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Person;
import no.nav.k9.søknad.felles.type.PersonIdent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class Barn implements Person {

    @JsonAlias({ "fødselsnummer", "norskIdentifikator", "norskIdentitetsnummer" })
    @JsonProperty(value = "identitetsnummer")
    @NotNull
    @Valid
    private NorskIdentitetsnummer identitetsnummer;

    @JsonProperty(value = "fødselsdato")
    @Valid
    private LocalDate fødselsdato;

    /** Hvorvidt barnet har samme bosted som søker. */
    @JsonProperty(value = "harSammeBosted")
    private Boolean harSammeBosted;

    @JsonCreator
    public Barn(@JsonProperty(value = "identitetsnummer", required = true) NorskIdentitetsnummer identitetsnummer,
                @JsonProperty(value = "fødselsdato") LocalDate fødselsdato,
                @JsonProperty(value = "harSammeBosted") Boolean harSammeBosted) {
        this.fødselsdato = fødselsdato;
        this.harSammeBosted = harSammeBosted;
        this.identitetsnummer = Objects.requireNonNull(identitetsnummer, "identitetsnummer");
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (Barn) obj;

        return Objects.equals(identitetsnummer, other.identitetsnummer)
            && Objects.equals(fødselsdato, other.fødselsdato);
    }

    @Override
    public PersonIdent getPersonIdent() {
        return identitetsnummer;
    }

    public Boolean getHarSammeBosted() {
        return harSammeBosted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identitetsnummer, fødselsdato);
    }
}