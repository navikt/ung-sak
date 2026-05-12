package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;

import java.time.LocalDate;

/**
 * Bostedavklaring for ett skjæringstidspunkt.
 * {@code fom} er fom-datoen i den tilhørende vilkårsperioden (= skjæringstidspunktet).
 * {@code kilde} angir om fakta er satt automatisk fra søknad eller manuelt av saksbehandler.
 */
public record BostedGrunnlagPeriodeDto(
    /** Fom-dato i vilkårsperioden. */
    @NotNull LocalDate fom,
    /** Om bruker er bosatt ved skjæringstidspunktet. */
    @NotNull Boolean erBosattITrondheim,
    /** Eventuell dato for utflytting fra Trondheim. Null dersom bruker er bosatt hele perioden. */
    LocalDate fraflyttingsDato,
    /** Årsak til fraflytting. Null dersom bruker er bosatt hele perioden. */
    FraflyttingsÅrsak fraflyttingsÅrsak,
    /** Kilde for fakta — SØKNAD (automatisk) eller SAKSBEHANDLER (manuelt registrert). */
    @NotNull Kilde kilde,
    /** Hva bruker oppga i søknaden. Null dersom ikke oppgitt. */
    Boolean søknadOppgittErBosattITrondheim,
    /** Om bruker har avgitt uttalelse om bosted. False dersom etterlysning ikke er besvart. */
    boolean harUttalelse,
    /** Brukerens uttalelsetekst. Null dersom bruker ikke har svart. */
    String uttalelseTekst
) {
}

