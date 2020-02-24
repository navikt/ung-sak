package no.nav.k9.sak.kontrakt.kontroll;

import javax.validation.constraints.NotNull;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE)
public class VurderFaresignalerDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "harInnvirketBehandlingen", required = true)
    @NotNull
    private Boolean harInnvirketBehandlingen;

    public VurderFaresignalerDto() {
        //
    }

    public VurderFaresignalerDto(String begrunnelse, Boolean harInnvirketBehandlingen) {
        super(begrunnelse);
        this.harInnvirketBehandlingen = harInnvirketBehandlingen;
    }

    public Boolean getHarInnvirketBehandlingen() {
        return harInnvirketBehandlingen;
    }

    public void setHarInnvirketBehandlingen(Boolean harInnvirketBehandlingen) {
        this.harInnvirketBehandlingen = harInnvirketBehandlingen;
    }
}
