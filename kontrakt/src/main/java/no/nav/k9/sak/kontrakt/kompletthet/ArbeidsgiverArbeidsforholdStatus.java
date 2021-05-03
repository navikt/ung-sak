package no.nav.k9.sak.kontrakt.kompletthet;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverArbeidsforholdStatus {

    @NotNull
    @Valid
    @JsonProperty(value = "arbeidsgiver")
    private ArbeidsgiverArbeidsforholdId arbeidsgiver;

    @NotNull
    @Valid
    @JsonProperty(value = "status")
    private Status status;

    public ArbeidsgiverArbeidsforholdStatus(@JsonProperty(value = "arbeidsgiver") ArbeidsgiverArbeidsforholdId arbeidsgiver, @JsonProperty(value = "status") Status status) {
        this.arbeidsgiver = arbeidsgiver;
        this.status = status;
    }

    public ArbeidsgiverArbeidsforholdId getArbeidsgiver() {
        return arbeidsgiver;
    }

    public Status getStatus() {
        return status;
    }
}
