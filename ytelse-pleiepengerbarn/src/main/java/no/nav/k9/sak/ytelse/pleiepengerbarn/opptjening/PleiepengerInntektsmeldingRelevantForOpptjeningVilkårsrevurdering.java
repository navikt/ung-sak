package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.AktivPeriodeForArbeidUtleder;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYtelsesstidslinjerForPermisjonvalidering;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Vurderer hvilke inntektsmeldinger som skal påvirke om vi skal revurdere opptjening. Denne tjenesten skal kun kalles i kontekst av en revurdering.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@VilkårTypeRef(VilkårType.OPPTJENINGSVILKÅRET)
public class PleiepengerInntektsmeldingRelevantForOpptjeningVilkårsrevurdering implements InntektsmeldingRelevantForVilkårsrevurdering {

    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private final MapYtelsesstidslinjerForPermisjonvalidering mapYtelsesstidslinjerForPermisjonvalidering = new MapYtelsesstidslinjerForPermisjonvalidering();

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public PleiepengerInntektsmeldingRelevantForOpptjeningVilkårsrevurdering() {
    }

    @Inject
    public PleiepengerInntektsmeldingRelevantForOpptjeningVilkårsrevurdering(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }


    /**
     * Finner inntektsmeldinger som er aktuelle i vurdering av om perioden skal revurderes pga mottatt inntektsmelding
     * <p>
     * Finner inntektsmeldinger for arbeidsforhold som er aktive dagen før skjæringstidspunktet
     *
     * @param referanse         Behandlingreferanse
     * @param inntektsmeldinger Inntektsmeldinger
     * @param periode           Vilkårsperiode
     * @return
     */
    @Override
    public List<Inntektsmelding> begrensInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet periode) {
        var relevanteImTjeneste = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, referanse.getFagsakYtelseType());
        var inntektsmeldingBegrenset = relevanteImTjeneste.begrensSakInntektsmeldinger(referanse, inntektsmeldinger, periode);
        var inntektsmeldingerTilBeregning = filtrerAktiveArbeidsforhold(referanse, periode, inntektsmeldingBegrenset);
        return relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingerTilBeregning, periode);

    }

    private Collection<Inntektsmelding> filtrerAktiveArbeidsforhold(BehandlingReferanse referanse,
                                                                    DatoIntervallEntitet periode, Collection<Inntektsmelding> inntektsmeldingForPeriode) {

        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.getBehandlingId());
        if (iayGrunnlag.isEmpty()) {
            return Collections.emptyList();
        }
        return finnInntektsmeldingerForAktiveArbeidsforholdVedSkjæringstidspunktet(referanse, periode, inntektsmeldingForPeriode, iayGrunnlag.get());
    }

    private List<Inntektsmelding> finnInntektsmeldingerForAktiveArbeidsforholdVedSkjæringstidspunktet(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode,
                                                                                                      Collection<Inntektsmelding> inntektsmeldingForPeriode,
                                                                                                      InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()))
            .før(vilkårsperiode.getFomDato().plusDays(1));
        var tidslinjePerYtelse = mapYtelsesstidslinjerForPermisjonvalidering.utledYtelsesTidslinjerForValideringAvPermisjoner(new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(behandlingReferanse.getAktørId())));
        return inntektsmeldingForPeriode.stream().filter(im -> erAktivVedStp(vilkårsperiode, yrkesaktivitetFilter, iayGrunnlag, tidslinjePerYtelse, im)).toList();
    }

    private static boolean erAktivVedStp(DatoIntervallEntitet vilkårsperiode,
                                         YrkesaktivitetFilter yrkesaktivitetFilter,
                                         InntektArbeidYtelseGrunnlag grunnlag,
                                         Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse,
                                         Inntektsmelding im) {
        var yrkesaktiviteter = yrkesaktivitetFilter.getYrkesaktiviteter()
            .stream().filter(ya -> ya.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .collect(Collectors.toSet());

        var dagenFørStp = DatoIntervallEntitet.fraOgMedTilOgMed(vilkårsperiode.getFomDato().minusDays(1), vilkårsperiode.getFomDato().minusDays(1));

        var erInaktivDagenFørStp = yrkesaktiviteter.stream().map(y -> AktivPeriodeForArbeidUtleder.utledAktivTidslinje(y, grunnlag, vilkårsperiode, tidslinjePerYtelse))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::coalesceRightHandSide))
            .intersection(dagenFørStp.toLocalDateInterval())
            .isEmpty();

        return !erInaktivDagenFørStp;
    }

}
