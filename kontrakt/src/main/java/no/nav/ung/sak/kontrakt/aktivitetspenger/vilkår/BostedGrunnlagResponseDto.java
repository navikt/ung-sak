package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Respons fra GET /behandling/bosatt.
 * Returnerer saksbehandlers foreslåtte og eventuelle fastsatte bostedavklaringer per periode.
 */
public record BostedGrunnlagResponseDto(
    @NotNull @Valid List<BostedGrunnlagPeriodeDto> perioder
) {
}
