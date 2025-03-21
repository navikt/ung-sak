package no.nav.ung.sak.kontrakt.etterlysning.oppgave.kontroll;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record RegisterInntektOppgaveDto(
    @NotNull @Valid @JsonProperty("aktørId") AktørId aktørId,
    @Valid @JsonProperty("referanse") UUID referanse,
    @NotNull @JsonProperty("frist") LocalDateTime frist,
    @Valid @JsonProperty("registerInntekter") RegisterInntekter registerInntekter
) {

    @JsonCreator
    public RegisterInntektOppgaveDto {
    }
}
