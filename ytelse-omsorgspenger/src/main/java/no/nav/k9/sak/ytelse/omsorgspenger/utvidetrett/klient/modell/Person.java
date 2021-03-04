package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.typer.AktørId;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class Person {

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Valid
    private AktørId aktørId;

    @JsonCreator
    public Person(@JsonProperty(value = "aktørId", required = true) AktørId aktørId) {
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (Person) obj;

        return Objects.equals(aktørId, other.aktørId);
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }
}
