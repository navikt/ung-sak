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
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    /** Begrunnelse for relevante fakta som er lagt til grunn i avklaringen og varsel */
    String begrunnelse,
    /** Om det skal sendes varsel til bruker for denne perioden. */
    boolean skalSendeVarsel,
    /** IkkeOppfyltÅrsak.ANNET må begrunnes med fritekst. Null for andre årsaker */
    String fritekstTilVarsel,
    /** Begrunnelse for hvorfor det ikke skal sendes varsel til bruker. */
    String begrunnelseIkkeVarsel
) {
}

