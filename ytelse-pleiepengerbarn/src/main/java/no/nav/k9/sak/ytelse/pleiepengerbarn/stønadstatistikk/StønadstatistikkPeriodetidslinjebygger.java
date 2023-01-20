package no.nav.k9.sak.ytelse.pleiepengerbarn.stønadstatistikk;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@Dependent
class StønadstatistikkPeriodetidslinjebygger {

    private UttakRestKlient uttakRestKlient;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;


    @Inject
    public StønadstatistikkPeriodetidslinjebygger(UttakRestKlient uttakRestKlient,
                                                  BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                  BeregningsresultatRepository beregningsresultatRepository) {
        this.uttakRestKlient = uttakRestKlient;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }


    LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> lagTidslinjeFor(Behandling behandling) {
        /*
         * Lager tidslinje for alle uttaksperioder (med helgedager fratrukket), med
         * tilhørende beregningsgrunnlag og beregningsresultatdata.
         */
        final LocalDateTimeline<UttaksperiodeInfo> uttaksperiodeTidslinje = toUttaksperiodeTidslinje(uttakRestKlient.hentUttaksplan(behandling.getUuid(), true));
        final LocalDateTimeline<UttaksperiodeInfo> uttaksperiodeUtenHelgerTidslinje = Hjelpetidslinjer.fjernHelger(uttaksperiodeTidslinje);

        final LocalDateTimeline<Beregningsgrunnlag> beregningsgrunnlagTidslinje = toBeregningsgrunnlagTidslinje(beregningsgrunnlagTjeneste.hentEksaktFastsattForAllePerioder(BehandlingReferanse.fra(behandling)));
        final LocalDateTimeline<List<BeregningsresultatAndel>> beregningsresultatTidslinje = toBeregningsresultatTidslinje(beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId()));

        final LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> mellomregning = uttaksperiodeUtenHelgerTidslinje.combine(beregningsgrunnlagTidslinje, (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2))), JoinStyle.LEFT_JOIN);

        return mellomregning.combine(beregningsresultatTidslinje, (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2))), JoinStyle.LEFT_JOIN);
    }

    private <T> T valueOrNull(LocalDateSegment<T> s) {
        return (s != null) ? s.getValue() : null;
    }


    private LocalDateTimeline<UttaksperiodeInfo> toUttaksperiodeTidslinje(Uttaksplan uttaksplan) {
        return new LocalDateTimeline<>(uttaksplan.getPerioder().entrySet().stream().map(e -> new LocalDateSegment<>(e.getKey().getFom(), e.getKey().getTom(), e.getValue())).toList());
    }

    private LocalDateTimeline<Beregningsgrunnlag> toBeregningsgrunnlagTidslinje(List<Beregningsgrunnlag> beregningsgrunnlagListe) {
        if (beregningsgrunnlagListe.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        var segmenter = beregningsgrunnlagListe.stream()
            .sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
            .map(b -> new LocalDateSegment<>(b.getSkjæringstidspunkt(), Tid.TIDENES_ENDE, b))
            .toList();

        return new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);
    }

    private LocalDateTimeline<List<BeregningsresultatAndel>> toBeregningsresultatTidslinje(Optional<BeregningsresultatEntitet> beregningsresultatEntitet) {
        if (beregningsresultatEntitet.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        return beregningsresultatEntitet.get().getBeregningsresultatAndelTimeline();
    }

    static class InformasjonTilStønadstatistikkHendelse {
        private UttaksperiodeInfo uttaksperiodeInfo;
        private Beregningsgrunnlag beregningsgrunnlag;
        private List<BeregningsresultatAndel> beregningsresultatAndeler;

        public InformasjonTilStønadstatistikkHendelse(UttaksperiodeInfo uttaksperiodeInfo,
                                                      Beregningsgrunnlag beregningsgrunnlag) {
            this.uttaksperiodeInfo = uttaksperiodeInfo;
            this.beregningsgrunnlag = beregningsgrunnlag;
        }

        public InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info, List<BeregningsresultatAndel> beregningsresultatAndeler) {
            this.uttaksperiodeInfo = info.uttaksperiodeInfo;
            this.beregningsgrunnlag = info.beregningsgrunnlag;
            this.beregningsresultatAndeler = beregningsresultatAndeler;
        }

        public UttaksperiodeInfo getUttaksperiodeInfo() {
            return uttaksperiodeInfo;
        }

        public Beregningsgrunnlag getBeregningsgrunnlag() {
            return beregningsgrunnlag;
        }

        public List<BeregningsresultatAndel> getBeregningsresultatAndeler() {
            return beregningsresultatAndeler;
        }
    }
}
