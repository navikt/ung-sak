package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.Collections;
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

    @JsonProperty(value = "arbeidsforholdTilFordeling", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FordelBeregningsgrunnlagArbeidsforholdDto> arbeidsforholdTilFordeling = new ArrayList<>();

    @JsonProperty(value = "fordelBeregningsgrunnlagPerioder", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FordelBeregningsgrunnlagPeriodeDto> fordelBeregningsgrunnlagPerioder = new ArrayList<>();

    public List<FordelBeregningsgrunnlagArbeidsforholdDto> getArbeidsforholdTilFordeling() {
        return Collections.unmodifiableList(arbeidsforholdTilFordeling);
    }

    public List<FordelBeregningsgrunnlagPeriodeDto> getFordelBeregningsgrunnlagPerioder() {
        return Collections.unmodifiableList(fordelBeregningsgrunnlagPerioder);
    }

    public void leggTilArbeidsforholdTilFordeling(FordelBeregningsgrunnlagArbeidsforholdDto arbeidsforholdTilFordeling) {
        this.arbeidsforholdTilFordeling.add(arbeidsforholdTilFordeling);
    }

    public void setArbeidsforholdTilFordeling(List<FordelBeregningsgrunnlagArbeidsforholdDto> arbeidsforholdTilFordeling) {
        this.arbeidsforholdTilFordeling = arbeidsforholdTilFordeling;
    }

    public void setFordelBeregningsgrunnlagPerioder(List<FordelBeregningsgrunnlagPeriodeDto> fordelBeregningsgrunnlagPerioder) {
        this.fordelBeregningsgrunnlagPerioder = fordelBeregningsgrunnlagPerioder;
    }
}
