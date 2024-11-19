package no.nav.ung.sak.kontrakt.søknadsfrist.aksjonspunkt;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE)
public class OverstyrtSøknadsfristDto extends OverstyringAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1)
    @JsonProperty(value = "avklarteKrav")
    private List<AvklartKravDto> avklarteKrav;

    public OverstyrtSøknadsfristDto() {
    }

    public OverstyrtSøknadsfristDto(Periode periode, String begrunnelse, List<AvklartKravDto> avklarteKrav) {
        super(periode, begrunnelse);
        this.avklarteKrav = avklarteKrav;
    }

    public List<AvklartKravDto> getAvklarteKrav() {
        return avklarteKrav;
    }

    @Override
    public String getAvslagskode() {
        return null;
    }

    @Override
    public boolean getErVilkarOk() {
        return false;
    }
}
