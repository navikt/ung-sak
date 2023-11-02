package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.math.BigDecimal;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class FinnNyKvoteTjeneste {


    static LocalDateTimeline<BigDecimal> finnNyKvoteTidslinje(Beregningsgrunnlag beregningsgrunnlag) {
        var endretKvoteSegmenter = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt() != null && p.getTotalUtbetalingsgradFraUttak() != null)
            .filter(p -> p.getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt().compareTo(p.getTotalUtbetalingsgradFraUttak()) > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsgrunnlagPeriodeFom(), p.getBeregningsgrunnlagPeriodeTom(), p.getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt()))
            .toList();
        return new LocalDateTimeline<>(endretKvoteSegmenter);

    }

}
