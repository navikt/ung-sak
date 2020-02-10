package no.nav.k9.sak.kontrakt.dokument;

import java.util.Objects;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.JournalpostId;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class JournalpostIdDto {

    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @JsonCreator
    public JournalpostIdDto(@NotNull @JsonProperty(value = "journalpostId", required = true) @Digits(integer = 18, fraction = 0) String journalpostId) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
    }

    @AbacAttributt("journalpostId")
    public JournalpostId getJournalpostId() {
        return new JournalpostId(journalpostId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof JournalpostIdDto)) {
            return false;
        }
        var other = (JournalpostIdDto) obj;
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
