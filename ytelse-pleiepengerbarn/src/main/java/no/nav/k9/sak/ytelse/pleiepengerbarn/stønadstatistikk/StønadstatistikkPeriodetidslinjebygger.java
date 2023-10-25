package no.nav.k9.sak.ytelse.pleiepengerbarn.stønadstatistikk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
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

        final LocalDateTimeline<BeregningsgrunnlagPeriode> beregningsgrunnlagTidslinje = toBeregningsgrunnlagPeriodeTidslinje(beregningsgrunnlagTjeneste.hentEksaktFastsattForAllePerioder(BehandlingReferanse.fra(behandling)));
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

    private LocalDateTimeline<BeregningsgrunnlagPeriode> toBeregningsgrunnlagPeriodeTidslinje(List<Beregningsgrunnlag> beregningsgrunnlagListe) {
        if (beregningsgrunnlagListe.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        
        assertSammeVerdierForSammeStp(beregningsgrunnlagListe);

        var stpTidslinje = toStpBeregningsgrunnlagTidslinje(beregningsgrunnlagListe);
        
        var periodeSegmenter = stpTidslinje.stream()
            .map(s -> toGjeldendeBeregningsgrunnlagPeriode(s))
            .flatMap(Collection::stream)
            .toList();

        return new LocalDateTimeline<>(periodeSegmenter);
    }

    private LocalDateTimeline<Beregningsgrunnlag> toStpBeregningsgrunnlagTidslinje(List<Beregningsgrunnlag> beregningsgrunnlagListe) {
        var stpSegmenter = beregningsgrunnlagListe.stream()
                .sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
                .map(b -> new LocalDateSegment<>(b.getSkjæringstidspunkt(), Tid.TIDENES_ENDE, b))
                .toList();
        var stpTidslinje = new LocalDateTimeline<>(stpSegmenter, StandardCombinators::coalesceRightHandSide);
        return stpTidslinje;
    }

    private static List<LocalDateSegment<BeregningsgrunnlagPeriode>> toGjeldendeBeregningsgrunnlagPeriode(LocalDateSegment<Beregningsgrunnlag> s) {
        return s.getValue().getBeregningsgrunnlagPerioder()
            .stream()
            .map(bp -> new LocalDateSegment<>(bp.getPeriode().getFomDato(),
                    minDate(bp.getPeriode().getTomDato(), s.getTom()),
                    bp)
            )
            .toList();
    }


    private static LocalDate minDate(LocalDate tomDato, LocalDate sTom) {
        return tomDato.isBefore(sTom) ? tomDato : sTom;
    }
    
    private void assertSammeVerdierForSammeStp(List<Beregningsgrunnlag> beregningsgrunnlagListe) {
        for (Beregningsgrunnlag bg : beregningsgrunnlagListe) {
            final long antallUlikeBruttoPerÅr = bg.getBeregningsgrunnlagPerioder()
                    .stream()
                    .map(BeregningsgrunnlagPeriode::getBruttoPrÅr)
                    .distinct()
                    .count();
            if (antallUlikeBruttoPerÅr > 1) {
                throw new IllegalStateException("Det skal ikke være mulig med ulik brutto-per-år på samme STP.");
            }
        }
    }

    private LocalDateTimeline<List<BeregningsresultatAndel>> toBeregningsresultatTidslinje(Optional<BeregningsresultatEntitet> beregningsresultatEntitet) {
        if (beregningsresultatEntitet.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        return beregningsresultatEntitet.get().getBeregningsresultatAndelTimeline();
    }

    static class InformasjonTilStønadstatistikkHendelse {
        private UttaksperiodeInfo uttaksperiodeInfo;
        private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;
        private List<BeregningsresultatAndel> beregningsresultatAndeler;

        public InformasjonTilStønadstatistikkHendelse(UttaksperiodeInfo uttaksperiodeInfo,
                                                      BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            this.uttaksperiodeInfo = uttaksperiodeInfo;
            this.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
        }

        public InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info, List<BeregningsresultatAndel> beregningsresultatAndeler) {
            this.uttaksperiodeInfo = info.uttaksperiodeInfo;
            this.beregningsgrunnlagPeriode = info.beregningsgrunnlagPeriode;
            this.beregningsresultatAndeler = beregningsresultatAndeler;
        }

        public UttaksperiodeInfo getUttaksperiodeInfo() {
            return uttaksperiodeInfo;
        }

        public BeregningsgrunnlagPeriode getBeregningsgrunnlagPeriode() {
            return beregningsgrunnlagPeriode;
        }

        public List<BeregningsresultatAndel> getBeregningsresultatAndeler() {
            return beregningsresultatAndeler;
        }
    }
}
