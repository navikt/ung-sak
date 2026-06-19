package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.typer.Periode;

/**
 * Bostedavklaring (faktagrunnlag) for en periode.
 */
public record BostedAvklaringDto(
    Periode foreslåttPeriode,
    /** Om bruker er bosatt i Trondheim i hele perioden. */
    @NotNull Boolean erBosatt,
    /** Årsak til fraflytting eller avslått periode. Null dersom bruker er bosatt hele perioden. */
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak
) {
}

