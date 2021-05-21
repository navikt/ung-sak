package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.ArbeidsforholdRef;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class KompletthetForBeregningTjeneste {

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    KompletthetForBeregningTjeneste() {
        // CDI
    }

    @Inject
    public KompletthetForBeregningTjeneste(@FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                           @FagsakYtelseTypeRef("PSB") InntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning,
                                           ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggFraRegister(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraRegisterFunction(arbeidsforholdTjeneste);

        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction);
    }

    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggFraGrunnlag(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraGrunnlagFunction(iayTjeneste,
            new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId()));

        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction);
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedlegg(BehandlingReferanse ref,
                                                                                BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction) {
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

        var tidslinje = new LocalDateTimeline<>(vilkårsPerioder.stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));

        var relevantePerioder = vilkårsPerioder.stream()
            .map(it -> utledRelevantPeriode(tidslinje, it))
            .collect(Collectors.toSet());

        // For alle relevanteperioder vurder kompletthet
        for (DatoIntervallEntitet periode : relevantePerioder) {
            var utledManglendeVedleggForPeriode = utledManglendeVedleggForPeriode(ref, inntektsmeldinger, periode, vilkårsPerioder, finnArbeidsforholdForIdentPåDagFunction);
            perioderMedManglendeVedlegg.putAll(utledManglendeVedleggForPeriode);
        }

        return perioderMedManglendeVedlegg;
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
        var relevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(intersection.getMinLocalDate().minusWeeks(4), intersection.getMaxLocalDate().plusWeeks(4));

        if (orginalRelevantPeriode.equals(relevantPeriode)) {
            return relevantPeriode;
        }

        return utledRelevantPeriode(tidslinje, relevantPeriode, false);
    }

    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledManglendeVedleggForPeriode(BehandlingReferanse ref, Set<Inntektsmelding> inntektsmeldinger,
                                                                                              DatoIntervallEntitet relevantPeriode,
                                                                                              Set<DatoIntervallEntitet> vilkårsPerioder,
                                                                                              BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction) {

        var result = new HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>>();
        var relevanteInntektsmeldinger = utledRelevanteInntektsmeldinger(inntektsmeldinger, relevantPeriode);
        var tilnternArbeidsforhold = new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId());
        var relevanteVilkårsperioder = vilkårsPerioder.stream().filter(it -> relevantPeriode.overlapper(it.getFomDato(), it.getTomDato())).collect(Collectors.toList());
        for (DatoIntervallEntitet periode : relevanteVilkårsperioder) {
            var arbeidsgiverSetMap = finnArbeidsforholdForIdentPåDagFunction.apply(ref, periode.getFomDato());
            utledManglendeInntektsmeldingerPerDag(result, relevanteInntektsmeldinger, periode, tilnternArbeidsforhold, arbeidsgiverSetMap);
        }
        return result;
    }

    public Set<Inntektsmelding> hentAlleUnikeInntektsmeldingerForFagsak(Saksnummer saksnummer) {
        return iayTjeneste.hentUnikeInntektsmeldingerForSak(saksnummer);
    }

    public List<Inntektsmelding> utledRelevanteInntektsmeldingerForPeriode(Set<Inntektsmelding> alleInntektsmeldingerPåSak, DatoIntervallEntitet periode) {
        var relevanteInntektsmeldinger = utledRelevanteInntektsmeldinger(alleInntektsmeldingerPåSak, periode);

        return inntektsmeldingerRelevantForBeregning.utledInntektsmeldingerSomGjelderForPeriode(relevanteInntektsmeldinger, periode);
    }

    private <V extends ArbeidsforholdRef> void utledManglendeInntektsmeldingerPerDag(HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>> result,
                                                                                     Set<Inntektsmelding> relevanteInntektsmeldinger,
                                                                                     DatoIntervallEntitet periode,
                                                                                     BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold,
                                                                                     Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            result.put(periode, List.of());
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
                    .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, entry.getKey().getIdentifikator(), it.getReferanse(), false))
                    .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
            result.put(periode, manglendeInntektsmeldinger);
        }
    }

    private Set<Inntektsmelding> utledRelevanteInntektsmeldinger(Set<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet relevantPeriode) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getStartDatoPermisjon().isEmpty() || relevantPeriode.inkluderer(im.getStartDatoPermisjon().orElseThrow()))
            .collect(Collectors.toSet());
    }
}
