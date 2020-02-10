package no.nav.k9.sak.kontrakt.behandling;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.Saksnummer;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class SaksnummerDto {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Size(max = 19)
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private final String saksnummer;

    @JsonCreator
    public SaksnummerDto(@JsonProperty("saksnummer") @NotNull @Size(max = 19) @Pattern(regexp = "^[a-zA-Z0-9]*$") String saksnummer) {
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
    }

    public SaksnummerDto(Saksnummer saksnummer) {
        this.saksnummer = saksnummer.getVerdi();
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getVerdi() {
        return new Saksnummer(saksnummer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' + "" + saksnummer + '>';
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

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer);
    }

}
