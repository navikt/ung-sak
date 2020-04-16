package no.nav.k9.sak.kontrakt.opptjening;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE)
public class AvklarOpptjeningsvilkårDto extends BekreftetAksjonspunktDto {

    @Valid
    @Size(max = 100)
    @JsonProperty(value = "vilkårPerioder", required = true)
    private List<AvklarOpptjeningsvilkåretDto> perioder;

    public AvklarOpptjeningsvilkårDto() {
    }

    public AvklarOpptjeningsvilkårDto(List<AvklarOpptjeningsvilkåretDto> perioder, String begrunnelse) {
        super(begrunnelse);
        this.perioder = perioder;
    }

    public List<AvklarOpptjeningsvilkåretDto> getPerioder() {
        return perioder;
    }

    public void setPerioder(List<AvklarOpptjeningsvilkåretDto> perioder) {
        this.perioder = perioder;
    }
}
