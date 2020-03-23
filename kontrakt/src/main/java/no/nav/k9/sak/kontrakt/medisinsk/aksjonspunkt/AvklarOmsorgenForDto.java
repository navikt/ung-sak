package no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE)
public class AvklarOmsorgenForDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "harOmsorgenFor", required = true)
    @NotNull
    private Boolean harOmsorgenFor;

    public AvklarOmsorgenForDto(String begrunnelse, @JsonProperty(value = "harOmsorgenFor", required = true) @NotNull Boolean harOmsorgenFor) {
        super(begrunnelse);
        this.harOmsorgenFor = harOmsorgenFor;
    }

    protected AvklarOmsorgenForDto() {
        //
    }

    public Boolean getHarOmsorgenFor() {
        return harOmsorgenFor;
    }
}
