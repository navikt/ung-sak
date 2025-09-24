package no.nav.ung.sak.typer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.api.IndexKey;

import java.util.Objects;

/**
 * Saksnummer refererer til saksnummer registret i GSAK.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Saksnummer implements IndexKey, Comparable<Saksnummer>{

    private static final String REGEXP = "^[\\p{Alnum}]+$";
    private static final java.util.regex.Pattern PATTERN = java.util.regex.Pattern.compile(REGEXP);

    @JsonValue
    @NotNull
    @Pattern(regexp = REGEXP, message = "Saksnummer [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String saksnummer; // NOSONAR

    protected Saksnummer() {
        //
    }

    @JsonCreator
    public Saksnummer(@NotNull @Pattern(regexp = REGEXP, message = "Saksnummer [${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String saksnummer) {
        String s = Objects.requireNonNull(nonEmpty(saksnummer), "saksnummer");
        if (!PATTERN.matcher(s).matches()) {
            throw new IllegalArgumentException("Ugyldig saksnummer:" + saksnummer);
        }
        this.saksnummer = s;
    }

    private String nonEmpty(String str) {
        return str==null || str.trim().isEmpty()?null: str.trim();
    }

    @Override
    public String getIndexKey() {
        return saksnummer;
    }

    public String getVerdi() {
        return saksnummer;
    }

    @StandardAbacAttributt(StandardAbacAttributtType.SAKSNUMMER)
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
        return saksnummer;
    }

    @Override
    public int compareTo(Saksnummer o) {
        return this.saksnummer.compareTo(o.saksnummer);
    }
}
