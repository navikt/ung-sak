package no.nav.k9.sak.typer;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.api.IndexKey;

/**
 * Saksnummer refererer til saksnummer registret i GSAK.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Saksnummer implements IndexKey, Comparable<Saksnummer>{

    @JsonValue
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "Saksnummer '${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String saksnummer; // NOSONAR

    protected Saksnummer() {
        //
    }

    @JsonCreator
    public Saksnummer(@NotNull @Pattern(regexp = "^[\\p{Alnum}]+$", message = "Saksnummer '${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String saksnummer) {
        Objects.requireNonNull(saksnummer, "saksnummer");
        this.saksnummer = saksnummer;
    }

    @Override
    public String getIndexKey() {
        return saksnummer;
    }

    public String getVerdi() {
        return saksnummer;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj instanceof Saksnummer)) {
            return false;
        }
        Saksnummer other = (Saksnummer) obj;
        return Objects.equals(saksnummer, other.saksnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + saksnummer + ">";
    }

    @Override
    public int compareTo(Saksnummer o) {
        return this.saksnummer.compareTo(o.saksnummer);
    }
}
