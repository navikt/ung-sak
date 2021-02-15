package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell;

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
public class Søker implements Person {

    @JsonAlias({ "fødselsnummer", "norskIdentifikator", "norskIdentitetsnummer" })
    @JsonProperty(value = "identitetsnummer", required = true)
    @NotNull
    @Valid
    private NorskIdentitetsnummer identitetsnummer;

    @JsonCreator
    public Søker(@JsonProperty(value = "identitetsnummer", required = true) NorskIdentitetsnummer identitetsnummer) {
        this.identitetsnummer = Objects.requireNonNull(identitetsnummer, "identitetsnummer");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (Søker) obj;

        return Objects.equals(identitetsnummer, other.identitetsnummer);
    }

    @Override
    public PersonIdent getPersonIdent() {
        return identitetsnummer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identitetsnummer);
    }
}