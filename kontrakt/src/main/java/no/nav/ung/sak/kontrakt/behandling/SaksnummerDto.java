package no.nav.ung.sak.kontrakt.behandling;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;
import no.nav.ung.sak.typer.Saksnummer;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class SaksnummerDto {
    public static final String DESC = "SAKSNUMMER";
    public static final String NAME = "saksnummer";

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Size(min = 5, max = 19)
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private final String saksnummer;

    public SaksnummerDto(Saksnummer saksnummer) {
        this.saksnummer = saksnummer.getVerdi();
    }

    @JsonCreator
    public SaksnummerDto(@JsonProperty("saksnummer") @NotNull @Size(max = 19) @Pattern(regexp = "^[a-zA-Z0-9]*$") String saksnummer) {
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (SaksnummerDto) obj;
        return Objects.equals(saksnummer, other.saksnummer);
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getVerdi() {
        return new Saksnummer(saksnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' + "" + saksnummer + '>';
    }

}
