package no.nav.ung.sak.felles.typer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.felles.IndexKey;
import no.nav.ung.sak.felles.abac.AppAbacAttributt;
import no.nav.ung.sak.felles.abac.AppAbacAttributtType;


import java.io.Serializable;
import java.util.Objects;

/**
 * DokumentId refererer til journalpost registret i Joark.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class DokumentId implements Serializable, IndexKey {

    private static final String GYLDIG = "^[\\p{Alnum}]+$";

    @JsonValue
    @NotNull
    @Size(max = 50, min = 3)
    @Pattern(regexp = GYLDIG, message = "dokumentId [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String dokumentId; // NOSONAR

    DokumentId() {
        //
    }

    public DokumentId(Long dokumentId) {
        Objects.requireNonNull(dokumentId, "dokumentId");
        this.dokumentId = Long.toString(dokumentId);
    }

    @JsonCreator
    public DokumentId(@NotNull @Size(max = 50, min = 3) @Pattern(regexp = GYLDIG, message = "dokumentId [${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String dokumentId) {
        this.dokumentId = Objects.requireNonNull(nonEmpty(dokumentId), "dokumentId");
    }

    private String nonEmpty(String str) {
        return str==null || str.trim().isEmpty()?null: str.trim();
    }

    public static boolean erGyldig(String input) {
        return java.util.regex.Pattern.matches(GYLDIG, input);
    }

    @Override
    public String getIndexKey() {
        return dokumentId;
    }

    public String getVerdi() {
        return dokumentId;
    }

    @AppAbacAttributt(AppAbacAttributtType.DOKUMENT_ID)
    public DokumentId getDokumentId() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        DokumentId other = (DokumentId) obj;
        return Objects.equals(dokumentId, other.dokumentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + dokumentId + ">";
    }

}
