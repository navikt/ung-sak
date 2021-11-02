package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static java.util.stream.Collectors.flatMapping;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMedÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.IkkeTattStillingTil;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingVurderingInput;
import no.nav.k9.sak.domene.arbeidsforhold.impl.LeggTilResultat;
import no.nav.k9.sak.domene.arbeidsforhold.impl.YtelsespesifikkeInntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmpManglendePåkrevdeInntektsmeldingerTjeneste implements YtelsespesifikkeInntektsmeldingTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(OmpManglendePåkrevdeInntektsmeldingerTjeneste.class);
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    private static final Environment ENV = Environment.current();
    private final boolean devLoggingPotensieltSensitivt = !ENV.isProd();

    @Inject
    public OmpManglendePåkrevdeInntektsmeldingerTjeneste(BehandlingRepository behandlingRepository,
                                                         @FagsakYtelseTypeRef("OMP") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                         TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erMottattInntektsmeldingUtenArbeidsforhold(InntektsmeldingVurderingInput input) {
        var result = new HashMap<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>>();
        var behandlingReferanse = input.getReferanse();

        var grunnlagOptional = Optional.ofNullable(input.getGrunnlag());
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

        var yrkArbeidsgiverArbeidsforhold = yrkesaktivitetFilter.getYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver,
                flatMapping(im -> Stream.of(im.getArbeidsforholdRef()), Collectors.toSet())));

        for (ArbeidsgiverArbeidsforhold imArbeidsgiverArbeidsforhold : arbeidsforholdSøktOmFravær) {
            var imArbeidsgiver = imArbeidsgiverArbeidsforhold.getArbeidsgiver();
            var yrkArbeidsforhold = yrkArbeidsgiverArbeidsforhold.getOrDefault(imArbeidsgiver, Set.of());
            var imArbeidsforhold = imArbeidsgiverArbeidsforhold.getArbeidsforhold();
            if (yrkArbeidsforhold.stream().noneMatch(imArbeidsforhold::gjelderFor)) {
                if (IkkeTattStillingTil.vurder(imArbeidsgiver, imArbeidsforhold, grunnlag)) {
                    var imArbeidsforholdRefs = Set.of(imArbeidsforhold);
                    if (rapportertInntektFraArbeidsgiver(fagsakPeriode, imArbeidsgiver, inntektFilter, yrkesaktivitetFilter)) {
                        LeggTilResultat.leggTil(result, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD, imArbeidsgiver, imArbeidsforholdRefs);
                        logger.info("Inntektsmelding med inntekt uten kjent arbeidsforhold: arbeidsforholdRef={}", imArbeidsforholdRefs);
                    } else {
                        logger.info("Inntektsmelding uten kjent arbeidsforhold & ingen rapportert inntekt: arbeidsforholdRef={}", imArbeidsforholdRefs);
                    }
                } else {
                    if (devLoggingPotensieltSensitivt) {
                        if (yrkArbeidsforhold.isEmpty()) {
                            logger.warn("Inntektsmelding arbeidsgiver (med fravær) matcher ingen arbeidsgivere fra yrkesaktivitet: im [{}, {}], yrk {}",
                                imArbeidsgiver, imArbeidsforhold, yrkArbeidsgiverArbeidsforhold);
                        } else {
                            logger.warn("Inntektsmelding vurderer ikke (er ikke spesifikt, eller har ingen overstyring). im [{}, {}], yrk {}",
                                imArbeidsgiver, imArbeidsforhold, yrkArbeidsgiverArbeidsforhold);
                        }
                    }
                }
            } else {
                if (devLoggingPotensieltSensitivt) {
                    logger.info("Inntektsmelding matchet yrkesaktivitet(er): im [{}, {}], yrk {}",
                        imArbeidsgiver, imArbeidsforhold, yrkArbeidsforhold.stream().filter(imArbeidsforhold::gjelderFor).collect(Collectors.toList()));
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
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(InntektsmeldingVurderingInput input) {
        var behandlingReferanse = input.getReferanse();
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
