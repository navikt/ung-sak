package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE)
public class FastsettBruttoBeregningsgrunnlagSNDto extends BekreftetBeregningsgrunnlagDto {

    @JsonProperty(value = "skjæringstidspunkt")
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "bruttoBeregningsgrunnlag", required = true)
    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer bruttoBeregningsgrunnlag;

    public FastsettBruttoBeregningsgrunnlagSNDto() {
        // For Jackson
    }

    public FastsettBruttoBeregningsgrunnlagSNDto(String begrunnelse, Integer bruttoBeregningsgrunnlag) {
        super(begrunnelse);
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }

    public Integer getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }

    public void setBruttoBeregningsgrunnlag(Integer bruttoBeregningsgrunnlag) {
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
