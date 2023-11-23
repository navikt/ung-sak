package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningStegPeriodeFilter;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.ytelse.beregning.OppdaterKvoteTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.NedjustertUttaksgrad;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class PSBOppdaterKvoteTjeneste implements OppdaterKvoteTjeneste {

    private BeregningStegPeriodeFilter beregningStegPeriodeFilter;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;


    private UttakTjeneste uttakTjeneste;

    public PSBOppdaterKvoteTjeneste() {
        // CDI
    }

    @Inject
    public PSBOppdaterKvoteTjeneste(BeregningStegPeriodeFilter beregningStegPeriodeFilter, BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste, MapInputTilUttakTjeneste mapInputTilUttakTjeneste, UttakTjeneste uttakTjeneste) {
        this.beregningStegPeriodeFilter = beregningStegPeriodeFilter;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
    }


    @Override
    public void oppdaterKvote(BehandlingReferanse referanse) {
//        var vurdertePerioder = beregningStegPeriodeFilter.filtrerPerioder(referanse, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG);
//
//        var fastsatteGrunnlag = beregningsgrunnlagTjeneste.hentEksaktFastsatt(referanse, vurdertePerioder.stream().map(PeriodeTilVurdering::getSkjæringstidspunkt).toList());
//
//        var endretKvoteTidslinje = fastsatteGrunnlag.stream().sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
//            .map(FinnNyKvoteTjeneste::finnNyKvoteTidslinje)
//            .reduce(LocalDateTimeline.empty(), LocalDateTimeline::crossJoin);
//
//        if (endretKvoteTidslinje.isEmpty()) {
//            return;
//        }

        var uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtOgMapRequest(referanse);

        uttakTjeneste.nedjusterSøkersUttaksgrad(uttaksgrunnlag);


    }

    private Map<LukketPeriode, NedjustertUttaksgrad> mapNedjustertUttaksgrad(LocalDateTimeline<BigDecimal> nedjustertUttaksgrad) {
        Map<LukketPeriode, NedjustertUttaksgrad> nedjustert = new HashMap<>();
        nedjustertUttaksgrad.stream().forEach(segment -> {
            LukketPeriode periode = new LukketPeriode(segment.getFom(), segment.getTom());
            nedjustert.put(periode, new NedjustertUttaksgrad(segment.getValue()));
        });
        return nedjustert;
    }


}
