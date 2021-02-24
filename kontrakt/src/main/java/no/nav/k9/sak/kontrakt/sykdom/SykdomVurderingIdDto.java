package no.nav.k9.sak.kontrakt.sykdom;

import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SykdomVurderingIdDto {

    public static final String DESC = "sykdomVurderingId";
    public static final String NAME = "sykdomVurderingId";

    @JsonProperty(value = NAME, required = true)
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String sykdomVurderingId;

    public SykdomVurderingIdDto(String sykdomVurderingId) {
        this.sykdomVurderingId = Objects.requireNonNull(sykdomVurderingId, NAME);
    }
    
    protected SykdomVurderingIdDto() {
        //
    }
    
    
    public String getSykdomVurderingId() {
        return sykdomVurderingId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (SykdomVurderingIdDto) obj;
        return Objects.equals(this.sykdomVurderingId, other.sykdomVurderingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sykdomVurderingId);
    }

    @Override
    public String toString() {
        return String.valueOf(sykdomVurderingId);
    }
}
