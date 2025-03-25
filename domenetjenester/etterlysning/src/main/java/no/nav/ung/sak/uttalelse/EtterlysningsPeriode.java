package no.nav.ung.sak.uttalelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.util.UUID;

public record EtterlysningsPeriode(LocalDateInterval periode, EtterlysningInfo etterlysningInfo, UUID iayGrunnlagUUID) {
}
