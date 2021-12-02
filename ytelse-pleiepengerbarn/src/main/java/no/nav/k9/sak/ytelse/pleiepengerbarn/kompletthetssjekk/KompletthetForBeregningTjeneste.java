package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.ArbeidsforholdRef;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidstidMappingInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

@ApplicationScoped
public class KompletthetForBeregningTjeneste {

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    KompletthetForBeregningTjeneste() {
        // CDI
    }

    @Inject
    public KompletthetForBeregningTjeneste(@FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                           @FagsakYtelseTypeRef("PSB") InntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning,
                                           @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                           ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           VilkårResultatRepository vilkårResultatRepository,
                                           PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste,
                                           BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository) {
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
    }

    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggFraRegister(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraRegisterFunction(arbeidsforholdTjeneste);

        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction, false);
    }

    /**
     * Gir ut alle potensielt påkrevde vedlegg uten vurdering mot arbeid.
     * Benyttes for rest-tjeneste hvor det senere legges på status om vedlegget er mottatt
     *
     * @param ref referanse til behandlingen
     * @return påkrevde vedlegg per periode
     */
    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAllePåkrevdeVedleggFraGrunnlag(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraGrunnlagFunction(iayTjeneste,
            new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId()));

        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction, true);
    }

    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggFraGrunnlag(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraGrunnlagFunction(iayTjeneste,
            new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId()));

        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction, false);
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedlegg(BehandlingReferanse ref,
                                                                                BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction, boolean skipVurderingMotArbeid) {
        var perioderMedManglendeVedlegg = new HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>>();

        // Utled vilkårsperioder
        var vilkårsPerioder = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .sorted(Comparator.comparing(DatoIntervallEntitet::getFomDato))
            .collect(Collectors.toCollection(TreeSet::new));

        if (vilkårsPerioder.isEmpty()) {
            return perioderMedManglendeVedlegg;
        }

        var inntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());
        var perioderFraSøknad = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(ref);
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(ref);

        var input = new InputForKompletthetsvurdering(skipVurderingMotArbeid, perioderFraSøknad, vurderteSøknadsperioder);

        var tidslinje = new LocalDateTimeline<>(vilkårsPerioder.stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));

        var relevantePerioder = vilkårsPerioder.stream()
            .map(it -> utledRelevantPeriode(tidslinje, it))
            .collect(Collectors.toSet());

        // For alle relevanteperioder vurder kompletthet
        for (DatoIntervallEntitet periode : relevantePerioder) {
            var utledManglendeVedleggForPeriode = utledManglendeVedleggForPeriode(ref, inntektsmeldinger, periode, vilkårsPerioder, input, finnArbeidsforholdForIdentPåDagFunction);
            perioderMedManglendeVedlegg.putAll(utledManglendeVedleggForPeriode);
        }

        return perioderMedManglendeVedlegg;
    }

    private LocalDateTimeline<Boolean> utledTidslinje(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (vilkårene.isEmpty()) {
            return new LocalDateTimeline<>(List.of(new LocalDateSegment<>(referanse.getFagsakPeriode().toLocalDateInterval(), true)));
        }
        var vilkåret = vilkårene.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        return vilkåret.map(vilkår -> new LocalDateTimeline<>(vilkår.getPerioder().stream()
                .map(VilkårPeriode::getPeriode)
                .map(DatoIntervallEntitet::toLocalDateInterval)
                .map(it -> new LocalDateSegment<>(it, true))
                .collect(Collectors.toList())))
            .orElseGet(() -> new LocalDateTimeline<>(List.of(new LocalDateSegment<>(referanse.getFagsakPeriode().toLocalDateInterval(), true))));
    }

    public DatoIntervallEntitet utledRelevantPeriode(BehandlingReferanse referanse, DatoIntervallEntitet periode) {
        var tidslinje = utledTidslinje(referanse);
        return utledRelevantPeriode(tidslinje, periode, true);
    }

    DatoIntervallEntitet utledRelevantPeriode(LocalDateTimeline<Boolean> tidslinje, DatoIntervallEntitet periode) {
        return utledRelevantPeriode(tidslinje, periode, true);
    }

    private DatoIntervallEntitet utledRelevantPeriode(LocalDateTimeline<Boolean> tidslinje, DatoIntervallEntitet periode, boolean justerStart) {
        DatoIntervallEntitet orginalRelevantPeriode = periode;
        if (justerStart) {
            orginalRelevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusWeeks(4), periode.getTomDato().plusWeeks(4));
        }

        if (tidslinje.isEmpty()) {
            return orginalRelevantPeriode;
        }
        var intersection = tidslinje.intersection(new LocalDateInterval(periode.getFomDato().minusWeeks(4), periode.getTomDato().plusWeeks(4)));
        if (intersection.isEmpty()) {
            return orginalRelevantPeriode;
        }
        var relevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(intersection.getMinLocalDate().minusWeeks(4), intersection.getMaxLocalDate().plusWeeks(4));

        if (orginalRelevantPeriode.equals(relevantPeriode)) {
            return relevantPeriode;
        }

        return utledRelevantPeriode(tidslinje, relevantPeriode, false);
    }

    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledManglendeVedleggForPeriode(BehandlingReferanse ref,
                                                                                              Set<Inntektsmelding> inntektsmeldinger,
                                                                                              DatoIntervallEntitet relevantPeriode,
                                                                                              Set<DatoIntervallEntitet> vilkårsPerioder,
                                                                                              InputForKompletthetsvurdering input,
                                                                                              BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction) {
        var result = new HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>>();
        var relevanteInntektsmeldinger = utledRelevanteInntektsmeldinger(inntektsmeldinger, relevantPeriode);
        var tilnternArbeidsforhold = new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId());
        var relevanteVilkårsperioder = vilkårsPerioder.stream()
            .filter(it -> relevantPeriode.overlapper(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toList());

        for (DatoIntervallEntitet periode : relevanteVilkårsperioder) {
            var arbeidsgiverSetMap = finnArbeidsforholdForIdentPåDagFunction.apply(ref, periode.getFomDato());
            var manglendeVedleggForPeriode = utledManglendeInntektsmeldingerPerDag(relevanteInntektsmeldinger, periode, tilnternArbeidsforhold, arbeidsgiverSetMap, input);

            result.put(periode, manglendeVedleggForPeriode);
        }
        return result;
    }

    private boolean harFraværFraArbeidetIPerioden(InputForKompletthetsvurdering input,
                                                  DatoIntervallEntitet periode,
                                                  ManglendeVedlegg manglendeVedlegg) {

        if (input.getSkalHoppeOverVurderingMotArbeid()) {
            return true;
        }

        var perioderFraSøknadene = input.getPerioderFraSøknadene();
        var kravDokumenter = input.getVurderteSøknadsperioder().keySet();
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.toLocalDateInterval(), true)));

        var arbeidstidInput = new ArbeidstidMappingInput(kravDokumenter,
            perioderFraSøknadene,
            timeline,
            null,
            null);
        var arbeidIPeriode = new MapArbeid().map(arbeidstidInput);

        return harFraværFraArbeidsgiverIPerioden(arbeidIPeriode, manglendeVedlegg);
    }

    private boolean harFraværFraArbeidsgiverIPerioden(List<Arbeid> arbeidIPeriode, ManglendeVedlegg at) {
        return arbeidIPeriode.stream()
            .filter(it -> UttakArbeidType.ARBEIDSTAKER.equals(UttakArbeidType.fraKode(it.getArbeidsforhold().getType())))
            .anyMatch(it -> Objects.equals(at.getArbeidsgiver(), utledIdentifikator(it)) && harFravær(it.getPerioder()));
    }

    private Arbeidsgiver utledIdentifikator(Arbeid it) {
        if (it.getArbeidsforhold().getOrganisasjonsnummer() != null) {
            return Arbeidsgiver.virksomhet(it.getArbeidsforhold().getOrganisasjonsnummer());
        } else if (it.getArbeidsforhold().getAktørId() != null) {
            return Arbeidsgiver.fra(new AktørId(it.getArbeidsforhold().getAktørId()));
        }
        return null;
    }

    private boolean harFravær(Map<LukketPeriode, ArbeidsforholdPeriodeInfo> perioder) {
        return perioder.values().stream().anyMatch(it -> !it.getJobberNormalt().equals(it.getJobberNå()));
    }

    public Set<Inntektsmelding> hentAlleUnikeInntektsmeldingerForFagsak(Saksnummer saksnummer) {
        return iayTjeneste.hentUnikeInntektsmeldingerForSak(saksnummer);
    }

    public List<KompletthetPeriode> hentKompletthetsVurderinger(BehandlingReferanse ref) {
        var grunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId());
        return grunnlag.map(BeregningsgrunnlagPerioderGrunnlag::getKompletthetPerioder).orElse(List.of());
    }

    public List<Inntektsmelding> utledInntektsmeldingerSomBenytteMotBeregningForPeriode(BehandlingReferanse referanse, Set<Inntektsmelding> alleInntektsmeldingerPåSak, DatoIntervallEntitet periode) {
        var inntektsmeldings = inntektsmeldingerRelevantForBeregning.begrensSakInntektsmeldinger(referanse, alleInntektsmeldingerPåSak, periode);
        return inntektsmeldingerRelevantForBeregning.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldings, periode);
    }

    private <V extends ArbeidsforholdRef> List<ManglendeVedlegg> utledManglendeInntektsmeldingerPerDag(Set<Inntektsmelding> relevanteInntektsmeldinger,
                                                                                                       DatoIntervallEntitet periode,
                                                                                                       BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold,
                                                                                                       Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger,
                                                                                                       InputForKompletthetsvurdering input) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            return List.of();
        } else {
            var prioriterteInntektsmeldinger = inntektsmeldingerRelevantForBeregning.utledInntektsmeldingerSomGjelderForPeriode(relevanteInntektsmeldinger, periode);

            for (Inntektsmelding inntektsmelding : prioriterteInntektsmeldinger) {
                if (påkrevdeInntektsmeldinger.containsKey(inntektsmelding.getArbeidsgiver())) {
                    final Set<V> arbeidsforhold = påkrevdeInntektsmeldinger.get(inntektsmelding.getArbeidsgiver());
                    if (inntektsmelding.gjelderForEtSpesifiktArbeidsforhold()) {
                        V matchKey = tilnternArbeidsforhold.apply(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef());
                        arbeidsforhold.remove(matchKey);
                    } else {
                        arbeidsforhold.clear();
                    }
                    if (arbeidsforhold.isEmpty()) {
                        påkrevdeInntektsmeldinger.remove(inntektsmelding.getArbeidsgiver());
                    }
                }
            }

            var manglendeInntektsmeldinger = påkrevdeInntektsmeldinger.entrySet()
                .stream()
                .map(entry -> entry.getValue()
                    .stream()
                    .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, entry.getKey(), it != null ? it.getReferanse() : null, false))
                    .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .filter(it -> harFraværFraArbeidetIPerioden(input, periode, it))
                .collect(Collectors.toList());
            return manglendeInntektsmeldinger;
        }
    }

    public Set<Inntektsmelding> utledRelevanteInntektsmeldinger(Set<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet relevantPeriode) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getStartDatoPermisjon().isPresent() && relevantPeriode.inkluderer(im.getStartDatoPermisjon().orElseThrow()))
            .collect(Collectors.toSet());
    }
}
