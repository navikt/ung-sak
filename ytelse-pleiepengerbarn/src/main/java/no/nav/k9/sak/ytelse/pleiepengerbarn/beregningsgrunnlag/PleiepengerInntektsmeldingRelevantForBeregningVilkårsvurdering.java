package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForBeregningVilkårsvurdering;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class PleiepengerInntektsmeldingRelevantForBeregningVilkårsvurdering implements InntektsmeldingRelevantForBeregningVilkårsvurdering {

    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjenester;

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private boolean skalFiltrereBasertPåAktiviteter;


    public PleiepengerInntektsmeldingRelevantForBeregningVilkårsvurdering() {
    }

    @Inject
    public PleiepengerInntektsmeldingRelevantForBeregningVilkårsvurdering(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                                                          @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjenester,
                                                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                                          @KonfigVerdi(value = "BG_FORLENGELSE_FILTRER_IM_FRA_AKTIVITETER", defaultVerdi = "false") boolean skalFiltrereBasertPåAktiviteter) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.opptjeningForBeregningTjenester = opptjeningForBeregningTjenester;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.skalFiltrereBasertPåAktiviteter = skalFiltrereBasertPåAktiviteter;
    }


    @Override
    public List<Inntektsmelding> begrensInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet periode) {
        var relevanteImTjeneste = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, referanse.getFagsakYtelseType());
        var inntektsmeldingBegrenset = relevanteImTjeneste.begrensSakInntektsmeldinger(referanse, inntektsmeldinger, periode);
        var inntektsmeldingForPeriode = relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingBegrenset, periode);
        if (!skalFiltrereBasertPåAktiviteter) {
            return inntektsmeldingForPeriode;
        }
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var inntektsmeldingerTilBeregning = filtrerForBeregningsaktiviteter(referanse, iayGrunnlag, periode, inntektsmeldingForPeriode);
        return inntektsmeldingerTilBeregning;
    }

    private List<Inntektsmelding> filtrerForBeregningsaktiviteter(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet periode, List<Inntektsmelding> inntektsmeldingForPeriode) {
        var opptjeningAktiviteter = OpptjeningForBeregningTjeneste.finnTjeneste(opptjeningForBeregningTjenester, referanse.getFagsakYtelseType()).hentEksaktOpptjeningForBeregning(referanse, iayGrunnlag, periode);
        return filtrerForAktiviteter(inntektsmeldingForPeriode, opptjeningAktiviteter);
    }

    static List<Inntektsmelding> filtrerForAktiviteter(List<Inntektsmelding> inntektsmeldingForPeriode, Optional<OpptjeningAktiviteter> opptjeningAktiviteter) {
        var aktiviterForBeregning = opptjeningAktiviteter.stream().flatMap(a -> a.getOpptjeningPerioder().stream()).toList();
        return inntektsmeldingForPeriode.stream().filter(im -> aktiviterForBeregning.stream().anyMatch(a -> harSammeArbeidsgiver(im, a))) // Foreløpig filtrerer vi kun på arbeidsgivernivå for å tillate reberegning ved endret arbeidsforholdID
            .toList();
    }

    private static boolean harSammeArbeidsgiver(Inntektsmelding im, OpptjeningAktiviteter.OpptjeningPeriode a) {
        return Objects.equals(im.getArbeidsgiver().getArbeidsgiverOrgnr(), a.getArbeidsgiverOrgNummer()) &&
            Objects.equals(im.getArbeidsgiver().getArbeidsgiverAktørId(), a.getArbeidsgiverAktørId());
    }

}
