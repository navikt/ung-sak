package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Oppgitt bostedsfaktum for en periode.
 */
public record BostedGrunnlagPeriodeDto(
    @NotNull LocalDate fom,
    LocalDate tom,
    @NotNull Boolean søknadOppgittErBosattITrondheim
) {
}
