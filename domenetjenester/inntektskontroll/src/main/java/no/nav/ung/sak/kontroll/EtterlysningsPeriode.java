package no.nav.ung.sak.kontroll;

import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.util.UUID;

public record EtterlysningsPeriode(LocalDateInterval periode, EtterlysningInfo etterlysningInfo, UUID iayGrunnlagUUID) {
}
