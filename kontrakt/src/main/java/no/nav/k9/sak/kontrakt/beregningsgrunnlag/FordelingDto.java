package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelingDto {

    @JsonProperty(value="fordelBeregningsgrunnlag", required = true)
    @NotNull
    @Valid
    private FordelBeregningsgrunnlagDto fordelBeregningsgrunnlag;

    public FordelBeregningsgrunnlagDto getFordelBeregningsgrunnlag() {
        return fordelBeregningsgrunnlag;
    }

    public void setFordelBeregningsgrunnlag(FordelBeregningsgrunnlagDto fordelBeregningsgrunnlag) {
        this.fordelBeregningsgrunnlag = fordelBeregningsgrunnlag;
    }
}
