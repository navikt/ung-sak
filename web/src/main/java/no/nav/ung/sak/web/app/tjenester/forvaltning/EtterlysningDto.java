package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.UUID;

public record EtterlysningDto(@NotNull Long id,
                              @NotNull EtterlysningStatus etterlysningStatus,
                              @NotNull DatoIntervallEntitet periode,
                              @NotNull EtterlysningType etterlysningType,
                              @NotNull UUID grunnlagreferanse) {
}
