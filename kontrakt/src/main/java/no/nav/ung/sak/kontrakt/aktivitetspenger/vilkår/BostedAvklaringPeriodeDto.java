package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

/**
 * Saksbehandlers fakta-avklaring for én vilkårsperiode om brukers bosted.
 */
public record BostedAvklaringPeriodeDto(
    @NotNull @Valid Periode periode,
    @NotNull @Valid BostedVurderingDto vurdering
) {
}
