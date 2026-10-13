package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDateTime;

public record AndreLivsoppholdsytelserVurderingDto(
    @NotNull
    VilkårType vilkårType,

    @NotNull
    @Valid
    Periode periode,

    @NotNull
    boolean erVilkårOppfylt,

    AndreLivsoppholdsytelserIkkeOppfyltÅrsak avslagsårsak,

    @NotNull
    boolean erManuellVurdering,

    String begrunnelse,

    String fritekstVurderingBrev,

    String ytelseNavn,

    @NotNull
    String vurdertAv,

    @NotNull
    LocalDateTime vurdertTidspunkt
) implements InngangsvilkårVurderingDto {

    public AndreLivsoppholdsytelserVurderingDto {
        if (vilkårType != VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR) {
            throw new IllegalArgumentException("Ugyldig vilkårType: " + vilkårType);
        }
    }
}
