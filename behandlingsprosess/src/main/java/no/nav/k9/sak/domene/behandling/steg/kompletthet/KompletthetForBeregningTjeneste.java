package no.nav.k9.sak.domene.behandling.steg.kompletthet;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
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
import no.nav.k9.sak.typer.Saksnummer;
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
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    KompletthetForBeregningTjeneste() {
        // CDI
    }

    @Inject
    public KompletthetForBeregningTjeneste(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                           @Any Instance<KompletthetFraværFilter> fraværFiltere,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                           BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.fraværFiltere = fraværFiltere;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
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

    public List<Inntektsmelding> utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(BehandlingReferanse referanse, Collection<Inntektsmelding> alleInntektsmeldingerPåSak, DatoIntervallEntitet periode) {
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


}
