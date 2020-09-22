package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.UUID;

import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;

public class StartBeregningInput {

    private final YtelsespesifiktGrunnlagDto ytelseGrunnlag;

    private final UUID bgReferanse;

    private final LocalDate skjæringstidspunkt;

    public StartBeregningInput(UUID bgReferanse, LocalDate skjæringstidspunkt, YtelsespesifiktGrunnlagDto ytelseGrunnlag) {
        this.bgReferanse = bgReferanse;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.ytelseGrunnlag = ytelseGrunnlag;
    }

    public YtelsespesifiktGrunnlagDto getYtelseGrunnlag() {
        return ytelseGrunnlag;
    }

    public UUID getBgReferanse() {
        return bgReferanse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

}
