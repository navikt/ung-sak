package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

/**
 * Saksbehandlers fakta-avklaring for hvorfor bruker ikke bor i Trondheim i en periode
 */
public record BostedFaktaavklaringPeriodeDto(
    @NotNull @Valid Periode periode,
    @NotNull @Valid BostedVurderingIkkeOppfyltDto vurdering,
    boolean skalIkkeSendeVarsel
) {

    public boolean skalSendeVarsel() {
        return !skalIkkeSendeVarsel;
    }

}
