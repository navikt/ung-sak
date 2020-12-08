package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.*;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.flatMapping;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmpManglendePåkrevdeInntektsmeldingerTjeneste implements YtelsespesifikkeInntektsmeldingTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(OmpManglendePåkrevdeInntektsmeldingerTjeneste.class);
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingRepository behandlingRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    @Inject
    public OmpManglendePåkrevdeInntektsmeldingerTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                         BehandlingRepository behandlingRepository,
                                                         @FagsakYtelseTypeRef("OMP") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                         TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse behandlingReferanse) {
        return Map.of();
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erMottattInntektsmeldingUtenArbeidsforhold(BehandlingReferanse behandlingReferanse) {
        var result = new HashMap<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>>();

        var grunnlagOptional = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId());
        if (grunnlagOptional.isEmpty()) {
            return result;
        }
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        var fraværFraInntektsmeldingerPåFagsak = trekkUtFraværTjeneste.fraværFraInntektsmeldingerPåFagsak(behandling);

        var arbeidsforholdSøktOmFravær = fraværFraInntektsmeldingerPåFagsak.stream()
            .map(it -> new ArbeidsgiverArbeidsforhold(it.getArbeidsgiver(), it.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : it.getArbeidsforholdRef()))
            .collect(Collectors.toSet());

        var grunnlag = grunnlagOptional.get();
        var fagsakPeriode = behandling.getFagsak().getPeriode();
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(behandling.getAktørId()));

        var arbeidsgiverArbeidsforholdMap = yrkesaktivitetFilter.getYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver,
                flatMapping(im -> Stream.of(im.getArbeidsforholdRef()), Collectors.toSet())));

        for (ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold : arbeidsforholdSøktOmFravær) {
            var arbeidsgiver = arbeidsgiverArbeidsforhold.getArbeidsgiver();
            var arbeidsforhold = arbeidsgiverArbeidsforholdMap.getOrDefault(arbeidsgiver, Set.of());
            var arbeidsforholdet = arbeidsgiverArbeidsforhold.getArbeidsforhold();
            if (arbeidsforhold.stream().noneMatch(arbeidsforholdet::gjelderFor)
                && IkkeTattStillingTil.vurder(arbeidsgiver, arbeidsforholdet, grunnlag)) {
                var arbeidsforholdRefs = Set.of(arbeidsforholdet);
                if (rapportertInntektFraArbeidsgiver(fagsakPeriode, arbeidsgiver, inntektFilter, yrkesaktivitetFilter)) {
                    LeggTilResultat.leggTil(result, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD, arbeidsgiver, arbeidsforholdRefs);
                    logger.info("Inntektsmelding med inntekt uten kjent arbeidsforhold: arbeidsforholdRef={}", arbeidsforholdRefs);
                } else {
                    logger.info("Inntektsmelding uten kjent arbeidsforhold & ingen rapportert inntekt: arbeidsforholdRef={}", arbeidsforholdRefs);
                }
            }
        }

        return result;
    }

    private boolean rapportertInntektFraArbeidsgiver(DatoIntervallEntitet fagsakPeriode, Arbeidsgiver arbeidsgiver, InntektFilter inntektFilter, YrkesaktivitetFilter filter) {
        var erIkkeRapportArbeidsforholdIAOrdningen = filter.getAlleYrkesaktiviteter()
            .stream()
            .noneMatch(it -> it.getArbeidsgiver().equals(arbeidsgiver));
        return erIkkeRapportArbeidsforholdIAOrdningen && erRapportertInntekt(fagsakPeriode, arbeidsgiver, inntektFilter);
    }

    private boolean erRapportertInntekt(DatoIntervallEntitet fagsakPeriode, Arbeidsgiver arbeidsgiver, InntektFilter inntektFilter) {
        return inntektFilter
            .filter(arbeidsgiver)
            .getAlleInntektPensjonsgivende()
            .stream()
            .map(Inntekt::getAlleInntektsposter)
            .flatMap(Collection::stream)
            .filter(it -> fagsakPeriode.overlapper(it.getPeriode()))
            .anyMatch(it -> InntektspostType.LØNN.equals(it.getInntektspostType()));
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        var fraværFraInntektsmeldingerPåFagsak = trekkUtFraværTjeneste.fraværFraInntektsmeldingerPåFagsak(behandling);
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandlingReferanse.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET);

        var result = new HashMap<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>>();
        var arbeidsgiverMap = new HashMap<DatoIntervallEntitet, Map<Arbeidsgiver, Set<InternArbeidsforholdRef>>>();

        for (DatoIntervallEntitet periode : perioderTilVurdering) {

            var arbeidsgiverSetHashMap = new HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>>();
            fraværFraInntektsmeldingerPåFagsak.stream()
                .filter(this::erIkkeNullTimer)
                .filter(it -> it.getAktivitetType().erArbeidstakerEllerFrilans())
                .filter(it -> periode.overlapper(it.getPeriode()))
                .forEach(it -> {
                    var key = it.getArbeidsgiver();
                    var idSet = arbeidsgiverSetHashMap.getOrDefault(key, new HashSet<>());
                    idSet.add(it.getArbeidsforholdRef());
                    arbeidsgiverSetHashMap.put(key, idSet);
                });
            arbeidsgiverMap.put(periode, arbeidsgiverSetHashMap);
        }

        arbeidsgiverMap.values()
            .stream()
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .filter(it -> it.getValue().contains(InternArbeidsforholdRef.nullRef()) && it.getValue().size() > 1)
            .forEach(it -> {
                logger.info("Inntektsmelding uten kjent arbeidsforhold: arbeidsforholdRef={}", it.getValue());
                LeggTilResultat.leggTil(result, AksjonspunktÅrsak.OVERGANG_ARBEIDSFORHOLDS_ID_UNDER_YTELSE, it.getKey(), it.getValue());
            });

        return result;
    }

    private boolean erIkkeNullTimer(no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode it) {
        return !Duration.ZERO.equals(it.getFraværPerDag());
    }
}
