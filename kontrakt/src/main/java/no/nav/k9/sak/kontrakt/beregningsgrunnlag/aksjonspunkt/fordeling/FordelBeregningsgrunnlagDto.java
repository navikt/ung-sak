package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BekreftetBeregningsgrunnlagDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelBeregningsgrunnlagDto extends BekreftetBeregningsgrunnlagDto {


    @JsonProperty(value = "endretBeregningsgrunnlagPerioder")
    @Valid
    @Size(max = 100)
    private List<FordelBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder;

    public FordelBeregningsgrunnlagDto() {
        //
    }

    public List<FordelBeregningsgrunnlagPeriodeDto> getEndretBeregningsgrunnlagPerioder() {
        return endretBeregningsgrunnlagPerioder;
    }

    public void setEndretBeregningsgrunnlagPerioder(List<FordelBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder) {
        this.endretBeregningsgrunnlagPerioder = endretBeregningsgrunnlagPerioder;
    }

}
