package no.nav.k9.sak.kontrakt.kompletthet;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Arbeidsgiver;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverArbeidsforholdIdV2 {

    @Valid
    @NotNull
    @JsonProperty(value = "arbeidsgiver")
    private Arbeidsgiver arbeidsgiver;

    @JsonProperty(value = "arbeidsforhold")
    @NotNull
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsforhold;


    @JsonCreator
    public ArbeidsgiverArbeidsforholdIdV2(@Valid
                                          @NotNull
                                          @JsonProperty(value = "arbeidsgiver") Arbeidsgiver arbeidsgiver,
                                          @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
                                          @JsonProperty(value = "arbeidsforhold") String arbeidsforhold) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforhold = arbeidsforhold;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsforhold() {
        return arbeidsforhold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsgiverArbeidsforholdIdV2 that = (ArbeidsgiverArbeidsforholdIdV2) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(arbeidsforhold, that.arbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforhold);
    }
}
