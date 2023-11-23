package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.math.BigDecimal;
import java.util.Comparator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;

@ApplicationScoped
public class FinnPerioderMedNedjustertUttaksgrad {
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    public FinnPerioderMedNedjustertUttaksgrad() {
        // CDI
    }

    @Inject
    public FinnPerioderMedNedjustertUttaksgrad(BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }


    public LocalDateTimeline<BigDecimal> finnTidslinje(BehandlingReferanse referanse) {
        var fastsatteGrunnlag = beregningsgrunnlagTjeneste.hentEksaktFastsattForAllePerioderInkludertAvslag(referanse);
        return fastsatteGrunnlag.stream().sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
            .map(FinnNyKvoteTjeneste::finnNyKvoteTidslinje)
            .reduce(LocalDateTimeline.empty(), LocalDateTimeline::crossJoin);


    }


}
