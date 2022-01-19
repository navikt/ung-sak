package no.nav.k9.sak.typer;

import java.io.Serializable;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.api.IndexKey;

/**
 * Journalpostid refererer til journalpost registret i Joark.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class JournalpostId implements Serializable, IndexKey {

    private static final String GYLDIG = "^[\\p{Alnum}]+$";

    @JsonValue
    @NotNull
    @Size(max = 50, min = 3)
    @Pattern(regexp = GYLDIG, message = "Saksnummer [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String journalpostId; // NOSONAR

    JournalpostId() {
        //
    }

    public JournalpostId(Long journalpostId) {
        Objects.requireNonNull(journalpostId, "journalpostId");
        this.journalpostId = Long.toString(journalpostId);
    }

    @JsonCreator
    public JournalpostId(@NotNull @Size(max = 50, min = 3) @Pattern(regexp = GYLDIG, message = "JournalpostId [${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String journalpostId) {
        this.journalpostId = Objects.requireNonNull(nonEmpty(journalpostId), "journalpostId");
    }

    private String nonEmpty(String str) {
        return str==null || str.trim().isEmpty()?null: str.trim();
    }

    public static boolean erGyldig(String input) {
        return java.util.regex.Pattern.matches(GYLDIG, input);
    }

    @Override
    public String getIndexKey() {
        return journalpostId;
    }

    public String getVerdi() {
        return journalpostId;
    }

    @AbacAttributt("journalpostId")
    public JournalpostId getJournalpostId() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        JournalpostId other = (JournalpostId) obj;
        return Objects.equals(journalpostId, other.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + journalpostId + ">";
    }

}
