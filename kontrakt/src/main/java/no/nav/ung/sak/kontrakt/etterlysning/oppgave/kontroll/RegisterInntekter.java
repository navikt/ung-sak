package no.nav.ung.sak.kontrakt.etterlysning.oppgave.kontroll;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record RegisterInntekter(
        @NotNull @Valid @JsonProperty("registerinntekterForArbeidOgFrilans") List<RegisterInntektATFL> registerinntekterForArbeidOgFrilans,
        @NotNull @Valid @JsonProperty("registerinntekterForYtelse") List<RegisterInntektYtelse> registerinntekterForYtelse) {

    @JsonCreator
    public RegisterInntekter {
    }
}
