package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Bostedavklaring for ett skjæringstidspunkt, med foreslått og eventuell fastsatt verdi.
 * {@code fom} er fom-datoen i den tilhørende vilkårsperioden.
 */
public record BostedGrunnlagPeriodeDto(
    /** Fom-dato i vilkårsperioden. */
    @NotNull LocalDate fom,
    /** Saksbehandlers foreslåtte vurdering fra VURDER_BOSTED-steget. */
    @NotNull Boolean foreslåttErBosattITrondheim,
    /** Fastsatt vurdering etter FASTSETT_BOSTED. Null dersom ikke fastsatt ennå. */
    Boolean fastsattErBosattITrondheim,
    /** Hva bruker oppga i søknaden. Null dersom ikke oppgitt. */
    Boolean søknadOppgittErBosattITrondheim,
    /** Om bruker har avgitt uttalelse om bosted. False dersom etterlysning ikke er besvart. */
    boolean harUttalelse,
    /** Brukerens uttalelsetekst. Null dersom bruker ikke har svart. */
    String uttalelseTekst
) {
}
