package no.nav.k9.sak.kontrakt.opptjening;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE)
public class AvklarOpptjeningsvilkårDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "vilkårPeriodeVurderinger")
    @Size(max = 100)
    @Valid
    @NotNull
    private List<VilkårPeriodeVurderingDto> vilkårPeriodeVurderinger;

    @Valid
    @Size(max = 100)
    @JsonProperty(value = "vilkårPerioder", required = true)
    private List<Periode> opptjeningPerioder;

    public AvklarOpptjeningsvilkårDto() {
    }

    public AvklarOpptjeningsvilkårDto(List<VilkårPeriodeVurderingDto> vilkårPeriodeVurderinger, List<Periode> opptjeningPerioder, String begrunnelse) {
        super(begrunnelse);
        this.vilkårPeriodeVurderinger = vilkårPeriodeVurderinger;
        this.opptjeningPerioder = opptjeningPerioder;
    }

    public List<VilkårPeriodeVurderingDto> getVilkårPeriodeVurderinger() {
        return vilkårPeriodeVurderinger;
    }

    public List<Periode> getOpptjeningPerioder() {
        return opptjeningPerioder;
    }
}
