package no.nav.ung.sak.kontroll;

import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.util.UUID;

public record InntektskontrollEtterlysningsPeriode(LocalDateInterval periode, InntektskontrollEtterlysningInfo inntektskontrollEtterlysningInfo, UUID iayGrunnlagUUID) {
}
