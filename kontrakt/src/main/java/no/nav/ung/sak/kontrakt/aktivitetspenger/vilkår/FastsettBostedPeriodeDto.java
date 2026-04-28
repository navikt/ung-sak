package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

/**
 * Saksbehandlers fastsetting av bosted for én vilkårsperiode etter mottatt uttalelse fra bruker.
 */
public record FastsettBostedPeriodeDto(
    @NotNull @Valid Periode periode,
    @NotNull Boolean foreslåttVurderingErGyldig,
    /** Ny vurdering brukes kun dersom {@code foreslåttVurderingErGyldig == false}. */
    @Valid BostedVurderingDto nyVurdering
) {
}
