package no.nav.k9.sak.domene.behandling.steg.kompletthet;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.UtledManglendeInntektsmeldingerFraGrunnlagFunction;
import no.nav.k9.sak.domene.arbeidsforhold.impl.FinnEksternReferanse;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.typer.ArbeidsforholdRef;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

@ApplicationScoped
public class KompletthetForBeregningTjeneste {

    private static Logger LOGGER = LoggerFactory.getLogger(KompletthetForBeregningTjeneste.class);

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private Instance<KompletthetFraværFilter> fraværFiltere;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private FagsakRepository fagsakRepository;

    KompletthetForBeregningTjeneste() {
        // CDI
    }

    @Inject
    public KompletthetForBeregningTjeneste(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                           @Any Instance<KompletthetFraværFilter> fraværFiltere,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                           VilkårResultatRepository vilkårResultatRepository,
                                           BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                           FagsakRepository fagsakRepository) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.fraværFiltere = fraværFiltere;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.fagsakRepository = fagsakRepository;
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

        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction, true, false);
    }

    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggFraGrunnlag(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraGrunnlagFunction(iayTjeneste,
            new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId()));

        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction, false, true);
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedlegg(BehandlingReferanse ref,
                                                                                BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction, boolean skipVurderingMotArbeid, boolean skalIgnorerePerioderFraInfotrygd) {
        var perioderMedManglendeVedlegg = new HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>>();

        // Utled vilkårsperioder
        var vilkårsPerioder = beregningsgrunnlagVilkårTjeneste.utledPerioderForKompletthet(ref, false, false, skalIgnorerePerioderFraInfotrygd)
            .stream()
            .sorted(Comparator.comparing(DatoIntervallEntitet::getFomDato))
            .collect(Collectors.toCollection(TreeSet::new));

        if (vilkårsPerioder.isEmpty()) {
            return perioderMedManglendeVedlegg;
        }

        var inntektsmeldinger = iayTjeneste.hentInntektsmeldingerKommetTomBehandling(ref.getSaksnummer(), ref.getBehandlingId());
        var journalpostIds = inntektsmeldinger.stream().map(Inntektsmelding::getJournalpostId).toList();
        LOGGER.info("Tar hensyn til inntektsmeldinger i kompletthetvurdering: " + journalpostIds);


        // For alle relevanteperioder vurder kompletthet
        for (DatoIntervallEntitet periode : vilkårsPerioder) {
            var utledManglendeVedleggForPeriode = utledManglendeVedleggForPeriode(ref, inntektsmeldinger, periode, finnArbeidsforholdForIdentPåDagFunction, skipVurderingMotArbeid);
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

        var stpMigrertFraInfotrygd = fagsakRepository.hentSakInfotrygdMigreringer(referanse.getFagsakId()).stream()
            .map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .min(Comparator.naturalOrder());

        return vilkåret.map(vilkår -> new LocalDateTimeline<>(vilkår.getPerioder().stream()
                .map(VilkårPeriode::getPeriode)
                .map(DatoIntervallEntitet::toLocalDateInterval)
                .map(it -> utvidPeriodeForPeriodeFraInfotrygd(it, stpMigrertFraInfotrygd))
                .collect(Collectors.toList()), StandardCombinators::coalesceRightHandSide))
            .orElseGet(() -> new LocalDateTimeline<>(List.of(new LocalDateSegment<>(referanse.getFagsakPeriode().toLocalDateInterval(), true))));
    }

    /**
     * I tilfelle der vilkårsperioden er migrert fra infotrygd må vi utvide relevant periode for at inntektsmeldinger lenger tilbake i tid skal vurderes som relevante.
     * Inntektsmeldinger for perioder fra infotrygd vil ha opprinnelig skjæringstidspunkt oppgitt i inntektsmeldingen og ikke i skjæringstidspunktet i k9-sak.
     * Vi sier her at vi ser på inntektsmeldinger som er 2 år og 4 mnd gamle.
     *
     * @param opprinneligVilkårsperiode Opprinnelig vilkårsperiode
     * @param stpMigrertFraInfotrygd    Skjæringstidspunkt som er migrert fra infotrygd
     * @return LocaldateSegment for relevant periode for vilkårsperiode
     */
    private LocalDateSegment<Boolean> utvidPeriodeForPeriodeFraInfotrygd(LocalDateInterval opprinneligVilkårsperiode, Optional<LocalDate> stpMigrertFraInfotrygd) {
        if (stpMigrertFraInfotrygd.map(opprinneligVilkårsperiode.getFomDato()::equals).orElse(false)) {
            var periode = new LocalDateInterval(opprinneligVilkårsperiode.getFomDato().minusYears(2), opprinneligVilkårsperiode.getTomDato());
            return new LocalDateSegment<>(periode, true);
        }
        return new LocalDateSegment<>(opprinneligVilkårsperiode, true);
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
        var intersection = tidslinje.intersection(new LocalDateInterval(orginalRelevantPeriode.getFomDato(), orginalRelevantPeriode.getTomDato()));
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
                                                                                              BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction,
                                                                                              boolean skipVurderingMotArbeid) {
        var result = new HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>>();
        var tilnternArbeidsforhold = new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId());

        var arbeidsgiverSetMap = finnArbeidsforholdForIdentPåDagFunction.apply(ref, relevantPeriode.getFomDato());
        var manglendeVedleggForPeriode = utledManglendeInntektsmeldingerPerDag(inntektsmeldinger, relevantPeriode, tilnternArbeidsforhold, arbeidsgiverSetMap, ref, skipVurderingMotArbeid);

        result.put(relevantPeriode, manglendeVedleggForPeriode);
        return result;
    }


    public Set<Inntektsmelding> hentAlleUnikeInntektsmeldingerForFagsak(Saksnummer saksnummer) {
        return iayTjeneste.hentUnikeInntektsmeldingerForSak(saksnummer);
    }

    public List<KompletthetPeriode> hentKompletthetsVurderinger(BehandlingReferanse ref) {
        var grunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId());
        return grunnlag.map(BeregningsgrunnlagPerioderGrunnlag::getKompletthetPerioder).orElse(List.of());
    }

    public List<Inntektsmelding> utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(BehandlingReferanse referanse, Set<Inntektsmelding> alleInntektsmeldingerPåSak, DatoIntervallEntitet periode) {
        var relevanteImTjeneste = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, referanse.getFagsakYtelseType());
        var inntektsmeldings = relevanteImTjeneste.begrensSakInntektsmeldinger(referanse, alleInntektsmeldingerPåSak, periode);
        return relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldings, periode);
    }

    private <V extends ArbeidsforholdRef> List<ManglendeVedlegg> utledManglendeInntektsmeldingerPerDag(Set<Inntektsmelding> inntektsmeldinger,
                                                                                                       DatoIntervallEntitet periode,
                                                                                                       BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold,
                                                                                                       Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger,
                                                                                                       BehandlingReferanse ref,
                                                                                                       boolean skipVurderingMotArbeid) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            return List.of();
        } else {
            var prioriterteInntektsmeldinger = utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(ref, inntektsmeldinger, periode);

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

            var fraværFilter = KompletthetFraværFilter.finnTjeneste(fraværFiltere, ref.getFagsakYtelseType());
            var manglendeInntektsmeldinger = påkrevdeInntektsmeldinger.entrySet()
                .stream()
                .map(entry -> entry.getValue()
                    .stream()
                    .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, entry.getKey(), it != null ? it.getReferanse() : null, false))
                    .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .filter(im -> skipVurderingMotArbeid ||
                    fraværFilter.harFraværFraArbeidetIPerioden(ref, periode, im))
                .peek(im -> fraværFilter.harFraværFraArbeidetIPerioden(ref, periode, im))
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
