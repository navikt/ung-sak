package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_NØDVENDIGHET)
public class VurderNødvendighetDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "perioder")
    @Valid
    @Size(min = 1)
    private List<VurderNødvendighetPeriodeDto> perioder;

    public VurderNødvendighetDto() {
    }

    public VurderNødvendighetDto(List<VurderNødvendighetPeriodeDto> perioder) {
        this.perioder = perioder;
    }

    public List<VurderNødvendighetPeriodeDto> getPerioder() {
        return perioder;
    }
}
