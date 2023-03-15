package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class FiltrerInntektsmeldingForBeregningInputOverstyring {


    private final Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private final InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public FiltrerInntektsmeldingForBeregningInputOverstyring(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                                              InntektArbeidYtelseTjeneste iayTjeneste) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.iayTjeneste = iayTjeneste;
    }


    public LocalDateTimeline<Set<Inntektsmelding>> finnGyldighetstidslinjeForInntektsmeldinger(BehandlingReferanse behandlingReferanse,
                                                                                               Set<Inntektsmelding> inntektsmeldingerForSak,
                                                                                               DatoIntervallEntitet periode) {
        var imTjeneste = finnInntektsmeldingForBeregningTjeneste(behandlingReferanse);
        var inntektsmeldingerForPeriode = imTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingerForSak, periode);
        var gyldighetFraAareg = finnGyldighetstidslinjeFraAktiveArbeidsforhold(behandlingReferanse, inntektsmeldingerForPeriode, periode);

        return gyldighetFraAareg.intersection(new LocalDateInterval(periode.getFomDato(), periode.getTomDato()));
    }

    private LocalDateTimeline<Set<Inntektsmelding>> finnGyldighetstidslinjeFraAktiveArbeidsforhold(BehandlingReferanse behandlingReferanse, List<Inntektsmelding> inntektsmeldingerForPeriode, DatoIntervallEntitet periode) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));
        var yrkesaktiviteter = yrkesaktivitetFilter.getYrkesaktiviteter();
        var inntektsmeldingsegmenter = inntektsmeldingerForPeriode.stream().flatMap(im ->
            yrkesaktiviteter.stream()
                .filter(it -> it.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
                .flatMap(it -> it.getAnsettelsesPeriode().stream())
                .map(AktivitetsAvtale::getPeriode)
                .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), Set.of(im)))).toList();
        return new LocalDateTimeline<>(inntektsmeldingsegmenter, StandardCombinators::union);
    }

    private InntektsmeldingerRelevantForBeregning finnInntektsmeldingForBeregningTjeneste(BehandlingReferanse behandlingReferanse) {
        FagsakYtelseType ytelseType = behandlingReferanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(inntektsmeldingerRelevantForBeregning, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + InntektsmeldingerRelevantForBeregning.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }


}
