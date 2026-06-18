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
    /** Fom-dato i vilkårsperioden. */
    @NotNull LocalDate fom,
    /** Tom-dato i vilkårsperioden. */
    LocalDate tom,
    /** Om bruker er bosatt ved skjæringstidspunktet. */
    @NotNull Boolean erBosattITrondheim,
    /** Årsak til fraflytting eller avslått periode. Null dersom bruker er bosatt hele perioden. */
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    /** Kilde for fakta — SØKNAD (automatisk) eller SAKSBEHANDLER (manuelt registrert). */
    @NotNull Kilde kilde,
    /** Hva bruker oppga i søknaden. Null dersom ikke oppgitt. */
    Boolean søknadOppgittErBosattITrondheim,
    /** Bostedavklaringen (faktagrunnlag) for perioden. */
    BostedAvklaringDto avklaring,
    /** Resultat av vurdering av bostedsvilkår. Null dersom ikke vurdert. */
    BostedResultatDto resultat,
    /** Om bruker har avgitt uttalelse om bosted. False dersom etterlysning ikke er besvart. */
    boolean harUttalelse,
    /** Brukerens uttalelsetekst. Null dersom bruker ikke har svart. */
    String uttalelseTekst
) {
}

