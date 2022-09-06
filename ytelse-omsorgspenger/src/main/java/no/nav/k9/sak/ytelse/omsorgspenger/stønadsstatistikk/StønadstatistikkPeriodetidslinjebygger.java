package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanForBehandlinger;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@Dependent
class StønadstatistikkPeriodetidslinjebygger {

    private ÅrskvantumRestKlient årskvantumRestKlient;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;


    @Inject
    public StønadstatistikkPeriodetidslinjebygger(ÅrskvantumRestKlient årskvantumRestKlient,
                                                  BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                  BeregningsresultatRepository beregningsresultatRepository,
                                                  VilkårResultatRepository vilkårResultatRepository) {
        this.årskvantumRestKlient = årskvantumRestKlient;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> lagTidslinjeFor(Behandling behandling) {
        /*
         * Lager tidslinje for alle uttaksperioder, med tilhørende beregningsgrunnlag, beregningsresultatdata og vilkår.
         */

        //begynn med uttaksplan, inkluderer også vilkår fra årskvantum
        FullUttaksplanForBehandlinger fullUttaksplanForBehandlinger = årskvantumRestKlient.hentFullUttaksplanForBehandling(List.of(behandling.getUuid()));
        LocalDateTimeline<UttakResultatPeriode> uttaksperiodeTidslinje = MapFraÅrskvantumResultat.getTimeline(fullUttaksplanForBehandlinger.getAktiviteter());

        //legg på beregningsgrunnlag
        LocalDateTimeline<BeregningsgrunnlagDto> beregningsgrunnlagTidslinje = toBeregningsgrunnlagTidslinje(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling)));
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medBeregningsgrunnlag = uttaksperiodeTidslinje.combine(beregningsgrunnlagTidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);

        //legg på beregningsresultat
        LocalDateTimeline<List<BeregningsresultatAndel>> beregningsresultatTidslinje = toBeregningsresultatTidslinje(beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId()));
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medBeregningesultat = medBeregningsgrunnlag.combine(beregningsresultatTidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);

        //legg på vilkår fra k9sak
        LocalDateTimeline<Map<VilkårType, Utfall>> k9sakVilkårtidslinje = lagVilkårTidslinje(vilkårResultatRepository.hent(behandling.getId()));
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medk9sakVilkår = medBeregningesultat.combine(k9sakVilkårtidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, datoSegment.getValue().kopiMedVilkårFraK9sak(valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);

        //legg på vilkår fra k9årskvantum
        LocalDateTimeline<Map<Vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall>> k9årskvantumVilkårtidslinje = MapFraÅrskvantumResultat.mapVilkårSomGjelderAlleAktiviteter(fullUttaksplanForBehandlinger.getAktiviteter());
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medk9årskvantumVilkår = medk9sakVilkår.combine(k9årskvantumVilkårtidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, datoSegment.getValue().kopiMedVilkårFraÅrskvantum(valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);
        return medk9årskvantumVilkår;
    }

    private static final Set<VilkårType> VILKÅR_TYPER_PR_KRAVSTILLER = Set.of(VilkårType.SØKNADSFRIST);

    private LocalDateTimeline<Map<VilkårType, Utfall>> lagVilkårTidslinje(Vilkårene vilkårene) {
        List<LocalDateSegment<Map<VilkårType, Utfall>>> segmenter = new ArrayList<>();
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer = vilkårene.getVilkårTidslinjer(DatoIntervallEntitet.fra(AbstractLocalDateInterval.TIDENES_BEGYNNELSE, AbstractLocalDateInterval.TIDENES_ENDE));
        for (Map.Entry<VilkårType, LocalDateTimeline<VilkårPeriode>> entry : vilkårTidslinjer.entrySet()) {
            if (VILKÅR_TYPER_PR_KRAVSTILLER.contains(entry.getKey())) {
                continue;
            }
            for (LocalDateSegment<VilkårPeriode> vilkårSegment : entry.getValue().toSegments()) {
                segmenter.add(new LocalDateSegment<>(vilkårSegment.getLocalDateInterval(), Map.of(entry.getKey(), vilkårSegment.getValue().getUtfall())));
            }
        }
        return new LocalDateTimeline<>(segmenter, (LocalDateInterval intervall, LocalDateSegment<Map<VilkårType, Utfall>> lhs, LocalDateSegment<Map<VilkårType, Utfall>> rhs) -> new LocalDateSegment<>(intervall, union(lhs.getValue(), rhs.getValue())));
    }

    private Map<VilkårType, Utfall> union(Map<VilkårType, Utfall> a, Map<VilkårType, Utfall> b) {
        Map<VilkårType, Utfall> resultat = new EnumMap<>(VilkårType.class);
        resultat.putAll(a);
        resultat.putAll(b);
        return resultat;
    }

    private <T> T valueOrNull(LocalDateSegment<T> s) {
        return (s != null) ? s.getValue() : null;
    }

    private LocalDateTimeline<BeregningsgrunnlagDto> toBeregningsgrunnlagTidslinje(List<BeregningsgrunnlagDto> beregningsgrunnlagListe) {
        if (beregningsgrunnlagListe.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        final List<LocalDateSegment<BeregningsgrunnlagDto>> segments = new ArrayList<>();
        for (int i = 0; i < beregningsgrunnlagListe.size(); i++) {
            final BeregningsgrunnlagDto b = beregningsgrunnlagListe.get(i);
            final LocalDate tom = (i + 1 < beregningsgrunnlagListe.size()) ? beregningsgrunnlagListe.get(i + 1).getSkjæringstidspunkt().minusDays(1) : Tid.TIDENES_ENDE;
            segments.add(new LocalDateSegment<>(b.getSkjæringstidspunkt(), tom, b));
        }
        return new LocalDateTimeline<>(segments);
    }

    private LocalDateTimeline<List<BeregningsresultatAndel>> toBeregningsresultatTidslinje(Optional<BeregningsresultatEntitet> beregningsresultatEntitet) {
        if (beregningsresultatEntitet.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        return beregningsresultatEntitet.get().getBeregningsresultatAndelTimeline();
    }

    static class InformasjonTilStønadstatistikkHendelse {

        private UttakResultatPeriode uttakresultat;

        //TODO søknadsfrist her

        private BeregningsgrunnlagDto beregningsgrunnlagDto;
        private List<BeregningsresultatAndel> beregningsresultatAndeler;

        Map<Vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall> vilkårFraÅrskvantum;
        private Map<VilkårType, Utfall> vilkårFraK9sak;

        private InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info) {
            //copy constructor
            uttakresultat = info.uttakresultat;
            beregningsgrunnlagDto = info.beregningsgrunnlagDto;
            beregningsresultatAndeler = info.beregningsresultatAndeler;
            vilkårFraÅrskvantum = info.vilkårFraÅrskvantum;
            vilkårFraK9sak = info.vilkårFraK9sak;
        }

        public InformasjonTilStønadstatistikkHendelse(UttakResultatPeriode uttakresultat, BeregningsgrunnlagDto beregningsgrunnlagDto) {
            this.uttakresultat = uttakresultat;
            this.beregningsgrunnlagDto = beregningsgrunnlagDto;
        }

        public InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info, List<BeregningsresultatAndel> beregningsresultatAndeler) {
            this(info);
            this.beregningsresultatAndeler = beregningsresultatAndeler;
        }

        public InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info, Map<VilkårType, Utfall> vilkårFraK9sak) {
            this(info);
            this.vilkårFraK9sak = vilkårFraK9sak;
        }

        public InformasjonTilStønadstatistikkHendelse kopiMedVilkårFraK9sak(Map<VilkårType, Utfall> vilkårFraK9sak) {
            InformasjonTilStønadstatistikkHendelse kopi = new InformasjonTilStønadstatistikkHendelse(this);
            kopi.vilkårFraK9sak = vilkårFraK9sak;
            return kopi;
        }

        public InformasjonTilStønadstatistikkHendelse kopiMedVilkårFraÅrskvantum(Map<Vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall> vilkårFraÅrskvantum) {
            InformasjonTilStønadstatistikkHendelse kopi = new InformasjonTilStønadstatistikkHendelse(this);
            kopi.vilkårFraÅrskvantum = vilkårFraÅrskvantum;
            return kopi;
        }

        public BeregningsgrunnlagDto getBeregningsgrunnlagDto() {
            return beregningsgrunnlagDto;
        }

        public List<BeregningsresultatAndel> getBeregningsresultatAndeler() {
            return beregningsresultatAndeler;
        }

        public UttakResultatPeriode getUttakresultat() {
            return uttakresultat;
        }

        public Map<VilkårType, Utfall> getVilkårFraK9sak() {
            return vilkårFraK9sak;
        }

        public Map<Vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall> getVilkårFraÅrskvantum() {
            return vilkårFraÅrskvantum;
        }
    }
}
