package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderVarigEndringEllerNyoppstartetDto extends BekreftetBeregningsgrunnlagDto {

    @JsonProperty(value = "bruttoBeregningsgrunnlag")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer bruttoBeregningsgrunnlag;

    @JsonProperty(value = "erVarigEndret")
    protected Boolean erVarigEndret;

    public VurderVarigEndringEllerNyoppstartetDto() {
        //
    }

    public VurderVarigEndringEllerNyoppstartetDto(@Valid @NotNull Periode periode) {
        super(periode);
    }

    public Integer getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }

    public void setBruttoBeregningsgrunnlag(Integer bruttoBeregningsgrunnlag) {
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }

    public Boolean getErVarigEndret() {
        return erVarigEndret;
    }

}
