package no.nav.k9.sak.ytelse.pleiepengerbarn.stønadstatistikk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.utils.Hjelpetidslinjer;
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
        final LocalDateTimeline<UttaksperiodeInfo> uttaksperiodeUtenHelgerTidslinje = uttaksperiodeTidslinje.disjoint(Hjelpetidslinjer.lagTidslinjeMedKunHelger(uttaksperiodeTidslinje));
        
        final LocalDateTimeline<BeregningsgrunnlagDto> beregningsgrunnlagTidslinje = toBeregningsgrunnlagTidslinje(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling)));
        final LocalDateTimeline<List<BeregningsresultatAndel>> beregningsresultatTidslinje = toBeregningsresultatTidslinje(beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId()));
        
        final LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> mellomregning = uttaksperiodeUtenHelgerTidslinje.combine(beregningsgrunnlagTidslinje, (datoInterval, datoSegment, datoSegment2) -> {
                return new LocalDateSegment<InformasjonTilStønadstatistikkHendelse>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2)));
            }, JoinStyle.LEFT_JOIN);
        
        return mellomregning.combine(beregningsresultatTidslinje, (datoInterval, datoSegment, datoSegment2) -> {
            return new LocalDateSegment<InformasjonTilStønadstatistikkHendelse>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2)));
        }, JoinStyle.LEFT_JOIN);
    }
    
    private <T> T valueOrNull(LocalDateSegment<T> s) {
        return (s != null) ? s.getValue() : null;
    }
    
    
    @SuppressWarnings("unchecked")
    private LocalDateTimeline<UttaksperiodeInfo> toUttaksperiodeTidslinje(Uttaksplan uttaksplan) {
        return new LocalDateTimeline<UttaksperiodeInfo>(uttaksplan.getPerioder().entrySet().stream().map(e -> new LocalDateSegment<UttaksperiodeInfo>(e.getKey().getFom(), e.getKey().getTom(), e.getValue())).toList());
    }
    
    @SuppressWarnings("unchecked")
    private LocalDateTimeline<BeregningsgrunnlagDto> toBeregningsgrunnlagTidslinje(List<BeregningsgrunnlagDto> beregningsgrunnlagListe) {
        if (beregningsgrunnlagListe.isEmpty()) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        }
        
        final List<LocalDateSegment<BeregningsgrunnlagDto>> segments = new ArrayList<>();
        for (int i=0; i<beregningsgrunnlagListe.size(); i++) {
            final BeregningsgrunnlagDto b = beregningsgrunnlagListe.get(i);
            final LocalDate tom = (i + 1 < beregningsgrunnlagListe.size()) ? beregningsgrunnlagListe.get(i+1).getSkjæringstidspunkt().minusDays(1) : Tid.TIDENES_ENDE;
            segments.add(new LocalDateSegment<>(b.getSkjæringstidspunkt(), tom, b));
        }
        return new LocalDateTimeline<>(segments);
    }
    
    @SuppressWarnings("unchecked")
    private LocalDateTimeline<List<BeregningsresultatAndel>> toBeregningsresultatTidslinje(Optional<BeregningsresultatEntitet> beregningsresultatEntitet) {
        if (beregningsresultatEntitet.isEmpty()) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        }
        
        return beregningsresultatEntitet.get().getBeregningsresultatAndelTimeline();
    }
    
    static class InformasjonTilStønadstatistikkHendelse {
        private UttaksperiodeInfo uttaksperiodeInfo;
        private BeregningsgrunnlagDto beregningsgrunnlagDto;
        private List<BeregningsresultatAndel> beregningsresultatAndeler;
        
        public InformasjonTilStønadstatistikkHendelse(UttaksperiodeInfo uttaksperiodeInfo,
                BeregningsgrunnlagDto beregningsgrunnlagDto) {
            this.uttaksperiodeInfo = uttaksperiodeInfo;
            this.beregningsgrunnlagDto = beregningsgrunnlagDto;
        }
        
        public InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info, List<BeregningsresultatAndel> beregningsresultatAndeler) {
            this.uttaksperiodeInfo = info.uttaksperiodeInfo;
            this.beregningsgrunnlagDto = info.beregningsgrunnlagDto;
            this.beregningsresultatAndeler = beregningsresultatAndeler;
        }
        
        public UttaksperiodeInfo getUttaksperiodeInfo() {
            return uttaksperiodeInfo;
        }
        
        public BeregningsgrunnlagDto getBeregningsgrunnlagDto() {
            return beregningsgrunnlagDto;
        }
        
        public List<BeregningsresultatAndel> getBeregningsresultatAndeler() {
            return beregningsresultatAndeler;
        }
    }
}
