package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_REISETID)
public class VurderReisetidDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "reisetid", required = true)
    @Size(min = 1)
    @Valid
    @NotNull
    private List<VurderReisetidPeriodeDto> reisetid;

    public VurderReisetidDto() {
    }

    public VurderReisetidDto(List<VurderReisetidPeriodeDto> reisetid) {
        this.reisetid = reisetid;
    }

    public List<VurderReisetidPeriodeDto> getReisetid() {
        return reisetid;
    }
}
