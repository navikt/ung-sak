package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AvbrytEtterlysningDto(@Min(0)
                                    @Max(Long.MAX_VALUE)
                                    @NotNull Long etterlysningId) {
}
