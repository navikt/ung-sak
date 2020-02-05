package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE)
public class VurderVarigEndringEllerNyoppstartetSNDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean erVarigEndretNaering;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer bruttoBeregningsgrunnlag;

    VurderVarigEndringEllerNyoppstartetSNDto() {
        // For Jackson
    }

    public VurderVarigEndringEllerNyoppstartetSNDto(String begrunnelse, boolean erVarigEndretNaering) {
        super(begrunnelse);
        this.erVarigEndretNaering = erVarigEndretNaering;
    }


    public boolean getErVarigEndretNaering() {
        return erVarigEndretNaering;
    }


    public Integer getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }

}
