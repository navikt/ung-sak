package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.validation.constraints.NotNull;

public record AvbrytEtterlysningDto(@NotNull Long etterlysningId) {
}
