package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDateTime;

public record GenerellInngangsvilkårVurderingDto(
    @NotNull
    VilkårType vilkårType,

    @NotNull
    @Valid
    Periode periode,

    boolean erVilkårOppfylt,

    String avslagsårsak,

    boolean erManuellVurdering,

    String begrunnelse,

    String fritekstVurderingBrev,

    String vurdertAv,

    LocalDateTime vurdertTidspunkt
) implements InngangsvilkårVurderingDto {
}
