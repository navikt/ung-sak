package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AndrePartSak implements Comparable<AndrePartSak>{

    @JsonValue
    @Valid
    @NotNull
    private Saksnummer saksnummer;

    @JsonCreator
    public AndrePartSak(Saksnummer saksnummer) {
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + saksnummer + ">";
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof AndrePartSak))
            return false;
        var other = (AndrePartSak) obj;
        return Objects.equals(this.saksnummer, other.saksnummer);
    }
    
    @Override
    public int compareTo(AndrePartSak o) {
        return saksnummer.compareTo(o.saksnummer);
    }
}
