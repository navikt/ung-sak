package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

import java.time.LocalDate;

/**
 * Bostedavklaring for en periode.
 * {@code kilde} angir om fakta er satt automatisk fra søknad eller manuelt av saksbehandler.
 */
public record BostedGrunnlagPeriodeDto(
    @NotNull LocalDate fom,
    LocalDate tom,
    @NotNull Boolean erBosattITrondheim,
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    @NotNull Kilde kilde,
    Boolean søknadOppgittErBosattITrondheim,
    /** Bostedavklaringen (faktagrunnlag) for perioden. Null dersom ikke avklart. */
    BostedAvklaringDto avklaring,
    /** Resultat av vurdering av bostedsvilkår. Null dersom ikke vurdert. */
    BostedResultatDto resultat,
    /** Om bruker har avgitt uttalelse om bosted. False dersom etterlysning ikke er besvart. */
    boolean harUttalelse,
    /** Brukerens uttalelsetekst. Null dersom bruker ikke har svart. */
    String uttalelseTekst
) {
}

