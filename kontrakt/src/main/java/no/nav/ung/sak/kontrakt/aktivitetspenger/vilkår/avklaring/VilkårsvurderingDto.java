package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record VilkårsvurderingDto(
    @NotNull boolean erVilkårOppfylt,
    String ikkeOppfyltÅrsak,
    String begrunnelse,
    String fritekstVurderingBrev,
    @NotNull boolean erManuellVurdering,
    @NotNull String vurdertAv,
    @NotNull LocalDateTime vurdertTidspunkt
) {
}
