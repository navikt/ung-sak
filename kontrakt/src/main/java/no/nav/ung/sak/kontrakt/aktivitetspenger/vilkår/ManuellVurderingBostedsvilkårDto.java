package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;

import java.util.List;

/**
 * DTO for aksjonspunkt 5140 – manuell vurdering av bostedsvilkåret.
 * Brukes ved årsak ANNET, mottatt uttalelse fra bruker, eller fakta fra søknad.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_BOSTEDVILKÅR_KODE)
public class ManuellVurderingBostedsvilkårDto extends BekreftetAksjonspunktDto {

    @JsonProperty("vurdertePerioder")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid VilkårPeriodeVurderingDto> vurdertePerioder;

    public ManuellVurderingBostedsvilkårDto() {
        // for Jackson
    }

    @JsonCreator
    public ManuellVurderingBostedsvilkårDto(@JsonProperty("vurdertePerioder") List<VilkårPeriodeVurderingDto> vurdertePerioder,
                                            @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VilkårPeriodeVurderingDto> getVurdertePerioder() {
        return vurdertePerioder;
    }
}
