package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.typer.Periode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "vilkårType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AndreLivsoppholdsytelserVurderingDto.class, name = InngangsvilkårVurderingDto.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR),
    @JsonSubTypes.Type(value = GenerellInngangsvilkårVurderingDto.class, names = {
        InngangsvilkårVurderingDto.BOSTEDSVILKÅR,
        InngangsvilkårVurderingDto.BISTANDSVILKÅR,
        InngangsvilkårVurderingDto.AKTIVITETSVILKÅR
    }),
})
public sealed interface InngangsvilkårVurderingDto permits AndreLivsoppholdsytelserVurderingDto, GenerellInngangsvilkårVurderingDto {

    String BOSTEDSVILKÅR = "AKT_VK_1";
    String BISTANDSVILKÅR = "AKT_VK_2";
    String ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR = "AKT_VK_4";
    String AKTIVITETSVILKÅR = "AKT_VK_5";

    VilkårType vilkårType();

    Periode periode();

    boolean erVilkårOppfylt();

    boolean erManuellVurdering();

    String begrunnelse();

    String fritekstVurderingBrev();
}
