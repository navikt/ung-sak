package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderFaktaOmBeregningDto extends BekreftetBeregningsgrunnlagDto {

    @JsonProperty(value = "fakta", required = true)
    @Valid
    @NotNull
    private FaktaBeregningLagreDto fakta;

    public VurderFaktaOmBeregningDto() {
        //
    }

    public FaktaBeregningLagreDto getFakta() {
        return fakta;
    }


}
