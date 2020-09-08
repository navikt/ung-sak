package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static java.util.stream.Collectors.flatMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMedÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.IkkeTattStillingTil;
import no.nav.k9.sak.domene.arbeidsforhold.impl.LeggTilResultat;
import no.nav.k9.sak.domene.arbeidsforhold.impl.YtelsespesifikkeInntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmpManglendePåkrevdeInntektsmeldingerTjeneste implements YtelsespesifikkeInntektsmeldingTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(OmpManglendePåkrevdeInntektsmeldingerTjeneste.class);
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingRepository behandlingRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    @Inject
    public OmpManglendePåkrevdeInntektsmeldingerTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                         BehandlingRepository behandlingRepository,
                                                         TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
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

        var yrkesaktivitetFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));

        var arbeidsgiverArbeidsforholdMap = yrkesaktivitetFilter.getAlleYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver,
                flatMapping(im -> Stream.of(im.getArbeidsforholdRef()), Collectors.toSet())));

        for (ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold : arbeidsforholdSøktOmFravær) {
            var arbeidsgiver = arbeidsgiverArbeidsforhold.getArbeidsgiver();
            var arbeidsforhold = arbeidsgiverArbeidsforholdMap.getOrDefault(arbeidsgiver, Set.of());
            var arbeidsforholdet = arbeidsgiverArbeidsforhold.getArbeidsforhold();
            if (!arbeidsforhold.contains(arbeidsforholdet) && IkkeTattStillingTil.vurder(arbeidsgiver, arbeidsforholdet, grunnlag)) {
                var arbeidsforholdRefs = Set.of(arbeidsforholdet);
                LeggTilResultat.leggTil(result, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD, arbeidsgiver, arbeidsforholdRefs);
                logger.info("Inntektsmelding uten kjent arbeidsforhold: arbeidsforholdRef={}", arbeidsforholdRefs);
            }
        }

        return result;
    }
}
