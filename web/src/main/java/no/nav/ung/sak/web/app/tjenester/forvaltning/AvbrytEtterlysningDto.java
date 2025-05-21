package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvbrytEtterlysningDto(@Size(max = 50)
                                    @NotNull String etterlysningId) {
}
