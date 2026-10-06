package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.util.List;

public record VilkårsavklaringerDto(
    @NotNull VilkårType vilkårType,
    @NotNull @Valid List<VilkårsavklaringDto> avklaringer
) {
}
