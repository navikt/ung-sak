package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE)
public class VurderVarigEndringEllerNyoppstartetSNDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "skjæringstidspunkt")
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "bruttoBeregningsgrunnlag")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer bruttoBeregningsgrunnlag;

    @JsonProperty(value = "erVarigEndretNaering", required = true)
    @NotNull
    private Boolean erVarigEndretNaering;

    public VurderVarigEndringEllerNyoppstartetSNDto() {
        //
    }

    public VurderVarigEndringEllerNyoppstartetSNDto(String begrunnelse, boolean erVarigEndretNaering) {
        super(begrunnelse);
        this.erVarigEndretNaering = erVarigEndretNaering;
    }


    public Integer getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }

    public void setBruttoBeregningsgrunnlag(Integer bruttoBeregningsgrunnlag) {
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }

    public boolean getErVarigEndretNaering() {
        return erVarigEndretNaering;
    }

    public void setErVarigEndretNaering(Boolean erVarigEndretNaering) {
        this.erVarigEndretNaering = erVarigEndretNaering;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
