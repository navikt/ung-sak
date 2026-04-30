package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Bostedavklaring for ett skjæringstidspunkt, med foreslått og eventuell fastsatt verdi.
 * {@code fom} er fom-datoen i den tilhørende vilkårsperioden (= skjæringstidspunktet).
 */
public record BostedGrunnlagPeriodeDto(
    /** Fom-dato i vilkårsperioden. */
    @NotNull LocalDate fom,
    /** Saksbehandlers foreslåtte vurdering: om bruker er bosatt ved skjæringstidspunktet. */
    @NotNull Boolean foreslåttErBosattITrondheim,
    /** Eventuell foreslått dato for utflytting fra Trondheim. Null dersom bruker er bosatt hele perioden. */
    LocalDate foreslåttFraflyttingsDato,
    /** Fastsatt vurdering etter FASTSETT_BOSTED. Null dersom ikke fastsatt ennå. */
    Boolean fastsattErBosattITrondheim,
    /** Eventuell fastsatt dato for utflytting fra Trondheim. Null dersom ikke fastsatt eller bosatt hele perioden. */
    LocalDate fastsattFraflyttingsDato,
    /** Hva bruker oppga i søknaden. Null dersom ikke oppgitt. */
    Boolean søknadOppgittErBosattITrondheim,
    /** Om bruker har avgitt uttalelse om bosted. False dersom etterlysning ikke er besvart. */
    boolean harUttalelse,
    /** Brukerens uttalelsetekst. Null dersom bruker ikke har svart. */
    String uttalelseTekst
) {
}

