package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Respons fra GET /behandling/bistand.
 * Returnerer saksbehandlers foreslåtte og eventuelle ferdigstilte bistandsavklaringer per periode,
 * sammen med vilkårsresultat og brukerens uttalelse.
 */
public record BistandGrunnlagResponseDto(
    @NotNull @Valid List<BistandGrunnlagPeriodeDto> perioder
) {
}
