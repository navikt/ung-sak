package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto extends BekreftetBeregningsgrunnlagDto {

    @JsonProperty(value = "bruttoBeregningsgrunnlag", required = true)
    @Min(0)
    @Max(Integer.MAX_VALUE)
    @NotNull
    private Integer bruttoBeregningsgrunnlag;

    public FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto() {
        // For Jackson
    }

    public FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(@Valid @NotNull Periode periode,
                                                                   @Min(0) @Max(Integer.MAX_VALUE) @NotNull Integer bruttoBeregningsgrunnlag) {
        super(periode);
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }

    public Integer getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }


}
