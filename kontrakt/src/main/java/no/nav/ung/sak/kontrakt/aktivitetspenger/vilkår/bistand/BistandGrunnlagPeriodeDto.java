package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Bistandsavklaring og vilkårsresultat for en periode.
 * I motsetning til bosted finnes det ingen søknadsfakta-dimensjon for bistand.
 */
public record BistandGrunnlagPeriodeDto(
    @NotNull LocalDate fom,
    LocalDate tom,
    /** Bistandsavklaringen (faktagrunnlag) for perioden. Null dersom ikke avklart. */
    BistandAvklaringDto avklaring,
    /** Resultat av vurdering av bistandsvilkåret. Null dersom ikke vurdert. */
    BistandResultatDto resultat,
    /** Settes hvis bruker har avgitt uttalelse om bistandsavklaringen. False dersom etterlysning ikke er besvart. */
    boolean harUttalelse,
    /** Brukerens uttalelsetekst. Null dersom bruker ikke har svart. */
    String uttalelseTekst
) {
}
