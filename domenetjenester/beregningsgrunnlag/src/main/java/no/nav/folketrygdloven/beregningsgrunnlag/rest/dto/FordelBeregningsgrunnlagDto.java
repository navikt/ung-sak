package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.util.ArrayList;
import java.util.List;


public class FordelBeregningsgrunnlagDto {

    private List<FordelBeregningsgrunnlagPeriodeDto> fordelBeregningsgrunnlagPerioder = new ArrayList<>();
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
