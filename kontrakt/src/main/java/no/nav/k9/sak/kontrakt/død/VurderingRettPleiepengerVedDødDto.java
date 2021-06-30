package no.nav.k9.sak.kontrakt.død;

import com.fasterxml.jackson.annotation.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.uttak.RettVedDødType;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_RETT_ETTER_PLEIETRENGENDES_DØD)
public class VurderingRettPleiepengerVedDødDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "vurdering")
    @Size(max = 4096)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String vurdering;

    @JsonProperty(value = "rettVedDødType")
    private RettVedDødType rettVedDødType;

    public VurderingRettPleiepengerVedDødDto() {
        //
    }

    public VurderingRettPleiepengerVedDødDto(String vurdering, RettVedDødType rettVedDødType) {
        this.vurdering = vurdering;
        this.rettVedDødType = rettVedDødType;
    }

    public String getVurdering() {
        return vurdering;
    }

    public RettVedDødType getRettVedDødType() {
        return rettVedDødType;
    }

}
