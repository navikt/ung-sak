package no.nav.ung.sak.kontrakt.etterlysning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record EndreFristDto(@NotNull
                            @Valid
                            UUID etterlysningEksternReferanse,
                            @NotNull
                            @Valid
                            LocalDate frist) {
}
