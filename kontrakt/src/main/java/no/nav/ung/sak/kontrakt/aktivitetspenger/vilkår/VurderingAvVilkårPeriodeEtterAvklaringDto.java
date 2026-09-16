package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;

public record VurderingAvVilkårPeriodeEtterAvklaringDto(

    @Valid
    @NotNull
    ÅpenPeriode periode,

    boolean erVilkårOppfylt,

    @NotNull
    @Size(min = 3, max = 5000)
    @Valid
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String begrunnelse,

    @Size(max = 10000)
    @Valid
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String fritekstVurderingBrev
) {
}
