package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
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
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkKravstillerType;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetTypeArbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.KravDokumentFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværHolder;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværVerdi;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@Dependent
class StønadstatistikkPeriodetidslinjebygger {

    private ÅrskvantumRestKlient årskvantumRestKlient;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    @Inject
    public StønadstatistikkPeriodetidslinjebygger(ÅrskvantumRestKlient årskvantumRestKlient,
                                                  BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                  BeregningsresultatRepository beregningsresultatRepository,
                                                  VilkårResultatRepository vilkårResultatRepository, TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.årskvantumRestKlient = årskvantumRestKlient;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
    }

    LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> lagTidslinjeFor(Behandling behandling) {
        /*
         * Lager tidslinje for alle uttaksperioder, med tilhørende beregningsgrunnlag, beregningsresultatdata og vilkår.
         */

        //begynn med uttaksplan
        FullUttaksplanForBehandlinger fullUttaksplanForBehandlinger = årskvantumRestKlient.hentFullUttaksplanForBehandling(List.of(behandling.getUuid()));
        LocalDateTimeline<UttakResultatPeriode> uttaksperiodeTidslinje = MapFraÅrskvantumResultat.getTimeline(fullUttaksplanForBehandlinger.getAktiviteter());

        //legg på beregningsgrunnlag
        LocalDateTimeline<Beregningsgrunnlag> beregningsgrunnlagTidslinje = toBeregningsgrunnlagTidslinje(beregningsgrunnlagTjeneste.hentEksaktFastsattForAllePerioder(BehandlingReferanse.fra(behandling)));
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medBeregningsgrunnlag = uttaksperiodeTidslinje.combine(beregningsgrunnlagTidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);

        //legg på beregningsresultat
        LocalDateTimeline<List<BeregningsresultatAndel>> beregningsresultatTidslinje = toBeregningsresultatTidslinje(beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId()));
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medBeregningesultat = medBeregningsgrunnlag.combine(beregningsresultatTidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, new InformasjonTilStønadstatistikkHendelse(datoSegment.getValue(), valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);

        //legg på vilkår fra k9sak
        LocalDateTimeline<Map<VilkårType, VilkårUtfall>> k9sakVilkårtidslinje = lagVilkårTidslinje(behandling);
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medk9sakVilkår = medBeregningesultat.combine(k9sakVilkårtidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, datoSegment.getValue().kopiMedVilkårFraK9sak(valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);

        //legg på vilkår fra k9årskvantum
        LocalDateTimeline<Map<Vilkår, VilkårUtfall>> k9årskvantumVilkårtidslinje = MapFraÅrskvantumResultat.mapVilkår(fullUttaksplanForBehandlinger.getAktiviteter());
        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> medk9årskvantumVilkår = medk9sakVilkår.combine(k9årskvantumVilkårtidslinje,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, datoSegment.getValue().kopiMedVilkårFraÅrskvantum(valueOrNull(datoSegment2)))
            , JoinStyle.LEFT_JOIN);

        LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> deduplisert = medk9årskvantumVilkår.mapValue(this::fjernDuplisertOmsorgenFor);

        LocalDateTimeline<Map<VilkårType, VilkårUtfall>> søknadsfristTidslinje = lagSøknadsfristTidslinjePrKravstiller(behandling);
        return deduplisert.combine(søknadsfristTidslinje,
            (datoInterval, lhs, rhs) -> new LocalDateSegment<>(datoInterval, lhs != null ? (rhs != null ? lhs.getValue().leggTilVilkårFraK9sak(rhs.getValue()) : lhs.getValue()) : new InformasjonTilStønadstatistikkHendelse().leggTilVilkårFraK9sak(rhs.getValue())),
            JoinStyle.CROSS_JOIN); //må ha CROSS_JOIN for å få med perioder med avslag i søknadsfrist. De blir ikke tatt med i vilkårsperiodene
    }

    InformasjonTilStønadstatistikkHendelse fjernDuplisertOmsorgenFor(InformasjonTilStønadstatistikkHendelse hendelse) {
        VilkårUtfall k9sakOmsorgenFor = hendelse.getVilkårFraK9sak() != null ? hendelse.getVilkårFraK9sak().get(VilkårType.OMSORGEN_FOR) : null;
        VilkårUtfall k9årskvantumOmsorgenFor = hendelse.getVilkårFraÅrskvantum() != null ? hendelse.getVilkårFraÅrskvantum().get(Vilkår.OMSORGSVILKÅRET) : null;
        if (k9sakOmsorgenFor == null || k9årskvantumOmsorgenFor == null) {
            //omsorgen for er ikke lagt til begge steder
            return hendelse;
        }
        if (k9sakOmsorgenFor.getUtfall() == Utfall.IKKE_VURDERT) {
            Map<VilkårType, VilkårUtfall> k9sakVilkår = new EnumMap<>(hendelse.getVilkårFraK9sak());
            k9sakVilkår.remove(VilkårType.OMSORGEN_FOR);
            return hendelse.kopiMedVilkårFraK9sak(k9sakVilkår);
        }
        if (k9årskvantumOmsorgenFor.getUtfall() == Utfall.IKKE_VURDERT) {
            Map<Vilkår, VilkårUtfall> k9årskvantumVilkår = new EnumMap<>(hendelse.getVilkårFraÅrskvantum());
            k9årskvantumVilkår.remove(Vilkår.OMSORGSVILKÅRET);
            return hendelse.kopiMedVilkårFraÅrskvantum(k9årskvantumVilkår);
        }
        throw new IllegalArgumentException("Kan ikke fjerne duplisert omsorgen for-vilkår, når vurdert i både k9-sak og k9-årskvantum");
    }

    private LocalDateTimeline<Map<VilkårType, VilkårUtfall>> lagVilkårTidslinje(Behandling behandling) {
        LocalDateTimeline<Map<VilkårType, VilkårUtfall>> tidslinje = lagVilkårTidslineFraVilkårResultat(behandling);

        //søknadsfrist vurderes pr kravstiller (strengt talt pr arbeidsforhold også, ved refusjon)
        LocalDateTimeline<Map<VilkårType, VilkårUtfall>> søknadsfristTidslinje = lagSøknadsfristTidslinjePrKravstiller(behandling);
        return tidslinje.combine(søknadsfristTidslinje, SEGMENT_KOMBINATOR_VILKÅR_UTFALL, JoinStyle.CROSS_JOIN);
    }


    private LocalDateTimeline<Map<VilkårType, VilkårUtfall>> lagVilkårTidslineFraVilkårResultat(Behandling behandling) {
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());
        List<LocalDateSegment<Map<VilkårType, VilkårUtfall>>> segmenter = new ArrayList<>();
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer = vilkårene.getVilkårTidslinjer(DatoIntervallEntitet.fra(AbstractLocalDateInterval.TIDENES_BEGYNNELSE, AbstractLocalDateInterval.TIDENES_ENDE));
        for (Map.Entry<VilkårType, LocalDateTimeline<VilkårPeriode>> entry : vilkårTidslinjer.entrySet()) {
            for (LocalDateSegment<VilkårPeriode> vilkårSegment : entry.getValue().toSegments()) {
                segmenter.add(new LocalDateSegment<>(vilkårSegment.getLocalDateInterval(), Map.of(entry.getKey(), new VilkårUtfall(vilkårSegment.getValue().getUtfall()))));
            }
        }
        return new LocalDateTimeline<>(segmenter, SEGMENT_KOMBINATOR_VILKÅR_UTFALL);

    }

    private LocalDateTimeline<Map<VilkårType, VilkårUtfall>> lagSøknadsfristTidslinjePrKravstiller(Behandling behandling) {
        var kravMedSøknadsfristvurdering = trekkUtFraværTjeneste.kravPåBehandlingenInklSøknadsfristvurdering(behandling);
        var kravMedSøknadsfristvurderingPrAktivitet = new KravDokumentFravær().trekkUtFravær(kravMedSøknadsfristvurdering);

        List<LocalDateSegment<Map<VilkårType, VilkårUtfall>>> segmenter = new ArrayList<>();
        kravMedSøknadsfristvurderingPrAktivitet.forEach((aktivitetTypeArbeidsgiver, tidslinje) ->
            tidslinje.stream().forEach(periode -> {
                OppgittFraværHolder oppgittFravær = periode.getValue();
                if (oppgittFravær.getSøknad() != null) {
                    segmenter.add(lagSegmentMedSøknadsfristvurderingSøknad(aktivitetTypeArbeidsgiver, periode.getLocalDateInterval(), oppgittFravær.getSøknad()));
                }
                oppgittFravær.getRefusjonskrav().forEach((arbeidsforhold, fraværverdi) ->
                    segmenter.add(lagSegmentMedSøknadsfristvurderingRefusjon(aktivitetTypeArbeidsgiver, periode.getLocalDateInterval(), arbeidsforhold, fraværverdi)));
            }));

        return new LocalDateTimeline<>(segmenter, SEGMENT_KOMBINATOR_VILKÅR_UTFALL);
    }


    private static LocalDateSegment<Map<VilkårType, VilkårUtfall>> lagSegmentMedSøknadsfristvurderingRefusjon(AktivitetTypeArbeidsgiver aktivitetTypeArbeidsgiver, LocalDateInterval intervall, InternArbeidsforholdRef arbeidsforhold, OppgittFraværVerdi fraværverdi) {
        return new LocalDateSegment<>(intervall, Map.of(VilkårType.SØKNADSFRIST, lagVilkårUtfallSøknadsfristRefusjon(fraværverdi.søknadsfristUtfall(), aktivitetTypeArbeidsgiver, arbeidsforhold)));
    }

    private static LocalDateSegment<Map<VilkårType, VilkårUtfall>> lagSegmentMedSøknadsfristvurderingSøknad(AktivitetTypeArbeidsgiver aktivitetTypeArbeidsgiver, LocalDateInterval intervall, OppgittFraværVerdi fraværverdi) {
        return new LocalDateSegment<>(intervall, Map.of(VilkårType.SØKNADSFRIST, lagVilkårUtfallSøknadsfristSøknad(fraværverdi.søknadsfristUtfall(), aktivitetTypeArbeidsgiver)));
    }

    private static VilkårUtfall lagVilkårUtfallSøknadsfristRefusjon(Utfall utfall, AktivitetTypeArbeidsgiver aktivitet, InternArbeidsforholdRef arbeidsforholdRef) {
        return new VilkårUtfall(utfall, Set.of(DetaljertVilkårUtfall.forKravstillerPerArbeidsforhold(utfall, StønadstatistikkKravstillerType.ARBEIDSGIVER, aktivitet.aktivitetType().getKode(), aktivitet.arbeidsgiver().getArbeidsgiverOrgnr(), aktivitet.arbeidsgiver().getArbeidsgiverAktørId(), arbeidsforholdRef.getReferanse())));
    }

    private static VilkårUtfall lagVilkårUtfallSøknadsfristSøknad(Utfall utfall, AktivitetTypeArbeidsgiver aktivitet) {
        return new VilkårUtfall(utfall, Set.of(DetaljertVilkårUtfall.forKravstillerPerArbeidsforhold(
            utfall,
            StønadstatistikkKravstillerType.BRUKER,
            aktivitet.aktivitetType().getKode(),
            aktivitet.arbeidsgiver() != null ? aktivitet.arbeidsgiver().getArbeidsgiverOrgnr() : null,
            aktivitet.arbeidsgiver() != null ? aktivitet.arbeidsgiver().getArbeidsgiverAktørId() : null,
            null)));
    }

    private static LocalDateSegmentCombinator<Map<VilkårType, VilkårUtfall>, Map<VilkårType, VilkårUtfall>, Map<VilkårType, VilkårUtfall>> SEGMENT_KOMBINATOR_VILKÅR_UTFALL = (LocalDateInterval intervall, LocalDateSegment<Map<VilkårType, VilkårUtfall>> lhs, LocalDateSegment<Map<VilkårType, VilkårUtfall>> rhs) -> new LocalDateSegment<>(intervall, nullSafeUnion(lhs, rhs));

    private static Map<VilkårType, VilkårUtfall> nullSafeUnion(LocalDateSegment<Map<VilkårType, VilkårUtfall>> lhs, LocalDateSegment<Map<VilkårType, VilkårUtfall>> rhs) {
        if (lhs != null && rhs != null) {
            return union(lhs.getValue(), rhs.getValue());
        }
        return lhs != null ? lhs.getValue() : rhs.getValue();
    }

    private static Map<VilkårType, VilkårUtfall> union(Map<VilkårType, VilkårUtfall> a, Map<VilkårType, VilkårUtfall> b) {
        Map<VilkårType, VilkårUtfall> resultat = new EnumMap<>(VilkårType.class);
        resultat.putAll(a);
        for (Map.Entry<VilkårType, VilkårUtfall> e : b.entrySet()) {
            if (resultat.containsKey(e.getKey())) {
                resultat.put(e.getKey(), kombinere(e.getValue(), resultat.get(e.getKey())));
            } else {
                resultat.put(e.getKey(), e.getValue());
            }
        }
        return resultat;
    }

    private static VilkårUtfall kombinere(VilkårUtfall a, VilkårUtfall b) {
        Utfall hovedutfall = a.getUtfall() == Utfall.OPPFYLT ? a.getUtfall() : b.getUtfall();
        if (a.getDetaljer() != null || b.getDetaljer() != null) {
            Set<DetaljertVilkårUtfall> detaljer = new LinkedHashSet<>();
            detaljer.addAll(a.getDetaljer() != null ? a.getDetaljer() : Set.of());
            detaljer.addAll(b.getDetaljer() != null ? b.getDetaljer() : Set.of());
            return new VilkårUtfall(hovedutfall, detaljer);
        } else {
            return new VilkårUtfall(hovedutfall);
        }
    }

    private <T> T valueOrNull(LocalDateSegment<T> s) {
        return (s != null) ? s.getValue() : null;
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

        private UttakResultatPeriode uttakresultat;

        private Beregningsgrunnlag beregningsgrunnlagDto;
        private List<BeregningsresultatAndel> beregningsresultatAndeler;

        Map<Vilkår, VilkårUtfall> vilkårFraÅrskvantum;
        private Map<VilkårType, VilkårUtfall> vilkårFraK9sak;

        private InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info) {
            //copy constructor
            uttakresultat = info.uttakresultat;
            beregningsgrunnlagDto = info.beregningsgrunnlagDto;
            beregningsresultatAndeler = info.beregningsresultatAndeler;
            vilkårFraÅrskvantum = info.vilkårFraÅrskvantum;
            vilkårFraK9sak = info.vilkårFraK9sak;
        }

        public InformasjonTilStønadstatistikkHendelse(UttakResultatPeriode uttakresultat, Beregningsgrunnlag beregningsgrunnlagDto) {
            this.uttakresultat = uttakresultat;
            this.beregningsgrunnlagDto = beregningsgrunnlagDto;
        }

        public InformasjonTilStønadstatistikkHendelse(InformasjonTilStønadstatistikkHendelse info, List<BeregningsresultatAndel> beregningsresultatAndeler) {
            this(info);
            this.beregningsresultatAndeler = beregningsresultatAndeler;
        }

        public InformasjonTilStønadstatistikkHendelse() {
        }

        public InformasjonTilStønadstatistikkHendelse kopiMedVilkårFraK9sak(Map<VilkårType, VilkårUtfall> vilkårFraK9sak) {
            InformasjonTilStønadstatistikkHendelse kopi = new InformasjonTilStønadstatistikkHendelse(this);
            kopi.vilkårFraK9sak = vilkårFraK9sak;
            return kopi;
        }

        public InformasjonTilStønadstatistikkHendelse leggTilVilkårFraK9sak(Map<VilkårType, VilkårUtfall> vilkårFraK9sak) {
            InformasjonTilStønadstatistikkHendelse kopi = new InformasjonTilStønadstatistikkHendelse(this);
            kopi.vilkårFraK9sak = VilkårKombinator.kombinerVilkår(this.getVilkårFraK9sak(), vilkårFraK9sak);
            return kopi;
        }


        public InformasjonTilStønadstatistikkHendelse kopiMedVilkårFraÅrskvantum(Map<Vilkår, VilkårUtfall> vilkårFraÅrskvantum) {
            InformasjonTilStønadstatistikkHendelse kopi = new InformasjonTilStønadstatistikkHendelse(this);
            kopi.vilkårFraÅrskvantum = vilkårFraÅrskvantum;
            return kopi;
        }

        public Beregningsgrunnlag getBeregningsgrunnlagDto() {
            return beregningsgrunnlagDto;
        }

        public List<BeregningsresultatAndel> getBeregningsresultatAndeler() {
            return beregningsresultatAndeler;
        }

        public UttakResultatPeriode getUttakresultat() {
            return uttakresultat;
        }

        public Map<VilkårType, VilkårUtfall> getVilkårFraK9sak() {
            return vilkårFraK9sak;
        }

        public Map<Vilkår, VilkårUtfall> getVilkårFraÅrskvantum() {
            return vilkårFraÅrskvantum;
        }
    }
}
