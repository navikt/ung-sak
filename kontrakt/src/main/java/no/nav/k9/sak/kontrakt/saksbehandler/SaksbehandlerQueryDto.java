package no.nav.k9.sak.kontrakt.saksbehandler;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SaksbehandlerQueryDto {
    @JsonProperty(value = "brukerid", required = true)
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
     private String brukerid;

    public SaksbehandlerQueryDto(String brukerid) {
        this.brukerid = brukerid;
    }

    public String getBrukerid() {
        return brukerid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SaksbehandlerQueryDto that = (SaksbehandlerQueryDto) o;
        return brukerid.equals(that.brukerid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukerid);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +  '<' + "" + brukerid + '>';
    }
}
