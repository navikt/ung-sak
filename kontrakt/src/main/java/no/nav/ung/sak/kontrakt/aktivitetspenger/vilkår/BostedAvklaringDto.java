package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

/**
 * Bostedavklaring (faktagrunnlag) for en periode.
 */
public record BostedAvklaringDto(
    /** Om bruker er bosatt i Trondheim i hele perioden. */
    @NotNull Boolean erBosatt,
    /** Årsak til fraflytting eller avslått periode. Null dersom bruker er bosatt hele perioden. */
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak
) {
}

