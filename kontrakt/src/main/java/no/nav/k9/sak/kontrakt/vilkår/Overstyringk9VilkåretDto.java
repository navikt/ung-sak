package no.nav.k9.sak.kontrakt.vilkår;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_K9_VILKÅRET_KODE)
public class Overstyringk9VilkåretDto extends OverstyringAksjonspunktDto {

    @JsonProperty("avslagskode")
    @Size(min = 4, max = 4)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String avslagskode;

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    @JsonProperty(value = "behandlingResultatType")
    @Valid
    private BehandlingResultatType behandlingResultatType;

    public Overstyringk9VilkåretDto() {
        //
    }

    public Overstyringk9VilkåretDto(Periode periode, boolean erVilkarOk, String begrunnelse, String avslagskode, BehandlingResultatType behandlingResultatType) { // NOSONAR
        super(periode, begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.avslagskode = avslagskode;
        this.behandlingResultatType = behandlingResultatType;
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }

    @Override
    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }
}
