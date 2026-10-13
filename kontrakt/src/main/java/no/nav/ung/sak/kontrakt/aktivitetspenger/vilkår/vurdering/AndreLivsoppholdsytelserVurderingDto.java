package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDateTime;

@JsonIgnoreProperties(value = "vilkårType", allowGetters = true)
public record AndreLivsoppholdsytelserVurderingDto(
    @NotNull
    @Valid
    Periode periode,

    boolean erVilkårOppfylt,

    AndreLivsoppholdsytelserIkkeOppfyltÅrsak avslagsårsak,

    boolean erManuellVurdering,

    String begrunnelse,

    String fritekstVurderingBrev,

    String ytelseNavn,

    String vurdertAv,

    LocalDateTime vurdertTidspunkt
) implements InngangsvilkårVurderingDto {

    @JsonProperty(value = "vilkårType", required = true)
    @Override
    public VilkårType vilkårType() {
        return VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
    }
}
