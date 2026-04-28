package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;

import java.time.LocalDate;

/**
 * Saksbehandlers vurdering av brukers bosted for én periode.
 * Brukes som felles undertype i {@link BostedAvklaringPeriodeDto} og {@link FastsettBostedPeriodeDto}.
 * <p>
 * Dersom {@code borITrondheimIHelePerioden} er {@code true}, er bruker bosatt i Trondheim hele perioden.
 * Dersom {@code borITrondheimIHelePerioden} er {@code false} og {@code fraflyttingsDato} er satt og etter
 * periodens fom-dato, deles perioden: fra fom bosatt, fra fraflyttingsDato ikke bosatt.
 * Dersom {@code fraflyttingsDato} er null eller ≤ fom, er bruker aldri bosatt i perioden.
 */
public record BostedVurderingDto(
    @NotNull Boolean borITrondheimIHelePerioden,
    LocalDate fraflyttingsDato,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String begrunnelse
) {
}
