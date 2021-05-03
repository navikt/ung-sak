package no.nav.k9.sak.kontrakt.kompletthet;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverArbeidsforholdId {

    @NotNull
    @JsonProperty(value = "arbeidsgiver")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiver;

    @JsonProperty(value = "arbeidsforhold")
    @NotNull
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsforhold;


    @JsonCreator
    public ArbeidsgiverArbeidsforholdId(@Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
                                        @JsonProperty(value = "arbeidsgiver") String arbeidsgiver,
                                        @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
                                        @JsonProperty(value = "arbeidsforhold") String arbeidsforhold) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforhold = arbeidsforhold;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsforhold() {
        return arbeidsforhold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsgiverArbeidsforholdId that = (ArbeidsgiverArbeidsforholdId) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(arbeidsforhold, that.arbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforhold);
    }
}
