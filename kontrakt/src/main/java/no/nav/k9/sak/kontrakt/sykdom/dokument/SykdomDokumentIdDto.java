package no.nav.k9.sak.kontrakt.sykdom.dokument;

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
public class SykdomDokumentIdDto {

    public static final String DESC = "sykdomDokumentId";
    public static final String NAME = "sykdomDokumentId";

    @JsonProperty(value = NAME, required = true)
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String sykdomdokumentId;

    public SykdomDokumentIdDto(String sykdomDokumentId) {
        this.sykdomdokumentId = Objects.requireNonNull(sykdomDokumentId, NAME);
    }
    
    protected SykdomDokumentIdDto() {
        //
    }
    
    
    public String getSykdomDokumentId() {
        return sykdomdokumentId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (SykdomDokumentIdDto) obj;
        return Objects.equals(this.sykdomdokumentId, other.sykdomdokumentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sykdomdokumentId);
    }

    @Override
    public String toString() {
        return String.valueOf(sykdomdokumentId);
    }
}
