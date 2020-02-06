package no.nav.k9.sak.kontrakt.opptjening;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE)
public class AvklarOpptjeningsvilkåretDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "avslagskode")
    @Size(min = 4, max = 4)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String avslagskode;

    @JsonProperty(value = "erVilkarOk")
    private boolean erVilkarOk;

    @SuppressWarnings("unused") // NOSONAR
    private AvklarOpptjeningsvilkåretDto() {
        super();
        // For Jackson
    }

    public AvklarOpptjeningsvilkåretDto(String begrunnelse, @Size(min = 4, max = 4) @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$") String avslagskode, boolean erVilkarOk) {
        super(begrunnelse);
        this.avslagskode = avslagskode;
        this.erVilkarOk = erVilkarOk;
    }


    public String getAvslagskode() {
        return avslagskode;
    }

    public boolean getErVilkarOk() {
        return erVilkarOk;
    }
}
