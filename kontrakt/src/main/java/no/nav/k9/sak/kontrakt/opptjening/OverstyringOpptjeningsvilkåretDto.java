package no.nav.k9.sak.kontrakt.opptjening;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE)
public class OverstyringOpptjeningsvilkåretDto extends OverstyringAksjonspunktDto {

    @JsonProperty("avslagskode")
    @Size(min = 4, max = 5)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String avslagskode;

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    @JsonProperty(value = "innvilgelseMerknadKode")
    @Size(min = 4, max = 5)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String innvilgelseMerknadKode;

    public OverstyringOpptjeningsvilkåretDto() {
        //
    }

    public OverstyringOpptjeningsvilkåretDto(Periode periode, boolean erVilkarOk, String begrunnelse, String avslagskode, String innvilgelseMerknadKode) {
        super(periode, begrunnelse);
        this.avslagskode = avslagskode;
        this.erVilkarOk = erVilkarOk;
        this.innvilgelseMerknadKode = innvilgelseMerknadKode;
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }

    public void setAvslagskode(String avslagskode) {
        this.avslagskode = avslagskode;
    }

    @Override
    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public void setErVilkarOk(boolean erVilkarOk) {
        this.erVilkarOk = erVilkarOk;
    }

    @Override
    public String getInnvilgelseMerknadKode() {
        return innvilgelseMerknadKode;
    }

    public void setInnvilgelseMerknadKode(String innvilgelseMerknadKode) {
        this.innvilgelseMerknadKode = innvilgelseMerknadKode;
    }
}
