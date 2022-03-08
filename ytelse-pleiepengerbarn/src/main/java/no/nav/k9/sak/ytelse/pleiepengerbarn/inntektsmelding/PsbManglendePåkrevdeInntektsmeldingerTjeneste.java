package no.nav.k9.sak.ytelse.pleiepengerbarn.inntektsmelding;

import static java.util.stream.Collectors.flatMapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMedÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.IkkeTattStillingTil;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingVurderingInput;
import no.nav.k9.sak.domene.arbeidsforhold.impl.LeggTilResultat;
import no.nav.k9.sak.domene.arbeidsforhold.impl.YtelsespesifikkeInntektsmeldingTjeneste;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("PPN")
public class PsbManglendePåkrevdeInntektsmeldingerTjeneste implements YtelsespesifikkeInntektsmeldingTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(PsbManglendePåkrevdeInntektsmeldingerTjeneste.class);
    private static final Environment ENV = Environment.current();
    private final boolean devLoggingPotensieltSensitivt = !ENV.isProd();
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private BehandlingRepository behandlingRepository;

    PsbManglendePåkrevdeInntektsmeldingerTjeneste() {
        // CDI
    }

    @Inject
    public PsbManglendePåkrevdeInntektsmeldingerTjeneste(BehandlingRepository behandlingRepository,
                                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                         KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
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

        var unikeInntektsmeldingerForFagsak = kompletthetForBeregningTjeneste.hentAlleUnikeInntektsmeldingerForFagsak(behandling.getFagsak().getSaksnummer());
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, input.getReferanse().getFagsakYtelseType(), input.getReferanse().getBehandlingType());
        var periodeTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var grunnlag = grunnlagOptional.get();
        var fagsakPeriode = behandling.getFagsak().getPeriode();
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(behandling.getAktørId()));

        var yrkArbeidsgiverArbeidsforhold = yrkesaktivitetFilter.getYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver,
                flatMapping(im -> Stream.of(im.getArbeidsforholdRef()), Collectors.toSet())));

        for (DatoIntervallEntitet periode : periodeTilVurdering) {
            var arbeidsforholdSøktOmFravær = kompletthetForBeregningTjeneste.utledInntektsmeldingerSomBenytteMotBeregningForPeriode(behandlingReferanse, unikeInntektsmeldingerForFagsak, periode)
                .stream()
                .map(it -> new ArbeidsgiverArbeidsforhold(it.getArbeidsgiver(), it.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : it.getArbeidsforholdRef()))
                .collect(Collectors.toSet());

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
        return Map.of();
    }
}
