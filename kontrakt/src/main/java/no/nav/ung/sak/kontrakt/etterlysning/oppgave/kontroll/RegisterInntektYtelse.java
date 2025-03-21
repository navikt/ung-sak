package no.nav.ung.sak.kontrakt.etterlysning.oppgave.kontroll;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record RegisterInntektYtelse(@NotNull @Valid @JsonProperty("beløp") Integer beløp, @NotNull @Valid @JsonProperty("ytelseType") FagsakYtelseType ytelseType) {

    @JsonCreator
    public RegisterInntektYtelse {
    }
}
