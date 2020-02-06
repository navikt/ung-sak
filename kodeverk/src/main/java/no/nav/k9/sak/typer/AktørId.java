package no.nav.k9.sak.typer;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.kodeverk.api.IndexKey;

/**
 * Id som genereres fra NAV Aktør Register. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker går fra
 * DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AktørId implements Serializable, Comparable<AktørId>, IndexKey {

    @JsonValue
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "AktørId '${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String aktørId; // NOSONAR

    protected AktørId() {
        // for hibernate
    }

    public AktørId(Long aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        this.aktørId = aktørId.toString();
    }

    @JsonCreator
    public AktørId(@NotNull @Size(max = 20) @Pattern(regexp = "^\\d+$", message = "AktørId '${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String aktørId) {
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
    }

    @Override
    public String getIndexKey() {
        return aktørId;
    }

    public String getId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        AktørId other = (AktørId) obj;
        return Objects.equals(aktørId, other.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + aktørId + ">";
    }

    @Override
    public int compareTo(AktørId o) {
        return aktørId.compareTo(o.aktørId);
    }

    private static AtomicLong DUMMY_AKTØRID = new AtomicLong(1000000000000L);

    /** Genererer dummy aktørid unikt for test. */
    public static AktørId dummy() {
        return new AktørId(DUMMY_AKTØRID.getAndIncrement());
    }
}
