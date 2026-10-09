package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.typer.Periode;

public record VilkårsavklaringRadDto(
    @NotNull @Valid Periode periode,
    @NotNull Utfall utfall,
    @Valid VilkårsavklaringDto avklaring
) {
}
