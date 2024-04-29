package no.nav.k9.sak.ytelse.pleiepengerbarn.inntektsmelding;

import static java.util.stream.Collectors.flatMapping;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
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
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class PsbManglendePåkrevdeInntektsmeldingerTjeneste implements YtelsespesifikkeInntektsmeldingTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(PsbManglendePåkrevdeInntektsmeldingerTjeneste.class);
    private static final Environment ENV = Environment.current();
    private final boolean devLoggingPotensieltSensitivt = !ENV.isProd();
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private BehandlingRepository behandlingRepository;

    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    private boolean reutledAksjonspunktVedRevurdering;

    PsbManglendePåkrevdeInntektsmeldingerTjeneste() {
        // CDI
    }

    @Inject
    public PsbManglendePåkrevdeInntektsmeldingerTjeneste(BehandlingRepository behandlingRepository,
                                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                         KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                                         VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                         @KonfigVerdi(value = "AVKLAR_ARBEIDSFORHOLD_REUTLED_VED_REVURDERING", defaultVerdi = "false") boolean reutledVedRevurdering) {
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.reutledAksjonspunktVedRevurdering = reutledVedRevurdering;
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
        var allePerioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
        if (reutledAksjonspunktVedRevurdering) {
            perioderTilVurdering = filtrerBortForlengelser(behandlingReferanse, allePerioderTilVurdering);
        } else {
            perioderTilVurdering = allePerioderTilVurdering;
        }

        var grunnlag = grunnlagOptional.get();
        var fagsakPeriode = behandling.getFagsak().getPeriode();
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(behandling.getAktørId()));

        Collection<Yrkesaktivitet> yrkesaktiviteter;
        if (reutledAksjonspunktVedRevurdering) {
            // Tar ikke med fiktive arbeidsforhold for å få opprettet aksjonspunktet på nytt ved tilbakerulling
            yrkesaktiviteter = yrkesaktivitetFilter.getYrkesaktiviteterEksklusiveFiktive();
        } else {
            yrkesaktiviteter = yrkesaktivitetFilter.getYrkesaktiviteter();
        }
        var yrkArbeidsgiverArbeidsforhold = yrkesaktiviteter
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver,
                flatMapping(im -> Stream.of(im.getArbeidsforholdRef()), Collectors.toSet())));

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var arbeidsforholdSøktOmFravær = kompletthetForBeregningTjeneste.utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(behandlingReferanse, unikeInntektsmeldingerForFagsak, periode)
                .stream()
                .map(it -> new ArbeidsgiverArbeidsforhold(it.getArbeidsgiver(), it.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : it.getArbeidsforholdRef()))
                .collect(Collectors.toSet());

            for (ArbeidsgiverArbeidsforhold imArbeidsgiverArbeidsforhold : arbeidsforholdSøktOmFravær) {
                var imArbeidsgiver = imArbeidsgiverArbeidsforhold.getArbeidsgiver();
                var yrkArbeidsforhold = yrkArbeidsgiverArbeidsforhold.getOrDefault(imArbeidsgiver, Set.of());
                var imArbeidsforhold = imArbeidsgiverArbeidsforhold.getArbeidsforhold();
                if (yrkArbeidsforhold.stream().noneMatch(imArbeidsforhold::gjelderFor)) {
                    if (reutledAksjonspunktVedRevurdering || IkkeTattStillingTil.vurder(imArbeidsgiver, imArbeidsforhold, grunnlag)) {
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

    private NavigableSet<DatoIntervallEntitet> filtrerBortForlengelser(BehandlingReferanse behandlingReferanse, NavigableSet<DatoIntervallEntitet> allePerioderTilVurdering) {
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
        var vilkårPeriodeFilter = vilkårPeriodeFilterProvider.getFilter(behandlingReferanse).ignorerForlengelseperioder();
        perioderTilVurdering = vilkårPeriodeFilter.filtrerPerioder(allePerioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR).stream()
            .map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new));
        return perioderTilVurdering;
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
