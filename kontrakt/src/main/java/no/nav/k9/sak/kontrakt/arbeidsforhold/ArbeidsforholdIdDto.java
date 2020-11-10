package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsforholdIdDto {

    @Valid
    @JsonProperty(value = "internArbeidsforholdId")
    private UUID internArbeidsforholdId;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String eksternArbeidsforholdId;

    @JsonCreator
    public ArbeidsforholdIdDto(@JsonProperty(value = "internArbeidsforholdId") @Valid UUID internArbeidsforholdId,
                               @JsonProperty(value = "eksternArbeidsforholdId") @Size(max = 100) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String eksternArbeidsforholdId) {
        this.internArbeidsforholdId = internArbeidsforholdId;
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    public UUID getInternArbeidsforholdId() {
        return internArbeidsforholdId;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    @Override
    public String toString() {
        return "ArbeidsforholdIdDto{" +
            "internArbeidsforholdId=" + internArbeidsforholdId +
            ", eksternArbeidsforholdId='" + eksternArbeidsforholdId + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsforholdIdDto that = (ArbeidsforholdIdDto) o;
        return Objects.equals(internArbeidsforholdId, that.internArbeidsforholdId) &&
            Objects.equals(eksternArbeidsforholdId, that.eksternArbeidsforholdId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internArbeidsforholdId, eksternArbeidsforholdId);
    }
}
