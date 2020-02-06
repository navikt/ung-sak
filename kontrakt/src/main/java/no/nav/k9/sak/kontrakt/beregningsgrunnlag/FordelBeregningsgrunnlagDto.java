package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelBeregningsgrunnlagDto {

    @JsonProperty(value = "fordelBeregningsgrunnlagPerioder", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FordelBeregningsgrunnlagPeriodeDto> fordelBeregningsgrunnlagPerioder = new ArrayList<>();

    @JsonProperty(value = "arbeidsforholdTilFordeling", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FordelBeregningsgrunnlagArbeidsforholdDto> arbeidsforholdTilFordeling = new ArrayList<>();

    public List<FordelBeregningsgrunnlagPeriodeDto> getFordelBeregningsgrunnlagPerioder() {
        return fordelBeregningsgrunnlagPerioder;
    }

    public void setFordelBeregningsgrunnlagPerioder(List<FordelBeregningsgrunnlagPeriodeDto> fordelBeregningsgrunnlagPerioder) {
        this.fordelBeregningsgrunnlagPerioder = fordelBeregningsgrunnlagPerioder;
    }

    public List<FordelBeregningsgrunnlagArbeidsforholdDto> getArbeidsforholdTilFordeling() {
        return arbeidsforholdTilFordeling;
    }

    public void leggTilArbeidsforholdTilFordeling(FordelBeregningsgrunnlagArbeidsforholdDto arbeidsforholdTilFordeling) {
        this.arbeidsforholdTilFordeling.add(arbeidsforholdTilFordeling);
    }
}
