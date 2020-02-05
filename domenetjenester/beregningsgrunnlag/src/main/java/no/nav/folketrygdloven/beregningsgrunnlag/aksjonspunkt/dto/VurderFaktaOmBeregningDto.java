package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE)
public class VurderFaktaOmBeregningDto extends BekreftetAksjonspunktDto {


    @Valid
    @NotNull
    private FaktaBeregningLagreDto fakta;

    public VurderFaktaOmBeregningDto() {
        // For jackson
    }

    public VurderFaktaOmBeregningDto(String begrunnelse, FaktaBeregningLagreDto fakta) {
        super(begrunnelse);
        this.fakta = fakta;
    }

    public FaktaBeregningLagreDto getFakta() {
        return fakta;
    }


}
