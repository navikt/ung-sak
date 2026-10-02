package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring;

import java.time.LocalDateTime;

public record VilkårsvurderingDto(
    boolean erVilkårOppfylt,
    String ikkeOppfyltÅrsak,
    String begrunnelse,
    String fritekstVurderingBrev,
    boolean erManuellVurdering,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
