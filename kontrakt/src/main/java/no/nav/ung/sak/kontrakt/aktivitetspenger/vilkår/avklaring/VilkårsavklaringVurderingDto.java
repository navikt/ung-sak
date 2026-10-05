package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record VilkårsavklaringVurderingDto(
    @NotNull @Valid VilkårsavklaringDto avklaring,
    @Valid VilkårsvurderingDto vurdering
) {
}
