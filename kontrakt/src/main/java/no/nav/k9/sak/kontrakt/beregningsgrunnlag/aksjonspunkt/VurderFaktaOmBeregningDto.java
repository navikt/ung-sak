package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;

import javax.validation.Valid;
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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE)
public class VurderFaktaOmBeregningDto extends BekreftetBeregningsgrunnlagDto {

    @JsonProperty(value = "skjæringstidspunkt")
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "fakta", required = true)
    @Valid
    @NotNull
    private FaktaBeregningLagreDto fakta;

    public VurderFaktaOmBeregningDto() {
        //
    }

    public VurderFaktaOmBeregningDto(String begrunnelse, FaktaBeregningLagreDto fakta) {
        super(begrunnelse);
        this.fakta = fakta;
    }

    public FaktaBeregningLagreDto getFakta() {
        return fakta;
    }

    public void setFakta(FaktaBeregningLagreDto fakta) {
        this.fakta = fakta;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
