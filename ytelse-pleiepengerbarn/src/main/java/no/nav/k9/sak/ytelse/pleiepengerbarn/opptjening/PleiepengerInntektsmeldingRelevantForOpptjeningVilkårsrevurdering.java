package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

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
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningInntektArbeidYtelseTjeneste;


    public PleiepengerInntektsmeldingRelevantForOpptjeningVilkårsrevurdering() {
    }

    @Inject
    public PleiepengerInntektsmeldingRelevantForOpptjeningVilkårsrevurdering(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                                                             OpptjeningInntektArbeidYtelseTjeneste opptjeningInntektArbeidYtelseTjeneste) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.opptjeningInntektArbeidYtelseTjeneste = opptjeningInntektArbeidYtelseTjeneste;
    }


    @Override
    public List<Inntektsmelding> begrensInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet periode) {
        var relevanteImTjeneste = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, referanse.getFagsakYtelseType());
        var inntektsmeldingBegrenset = relevanteImTjeneste.begrensSakInntektsmeldinger(referanse, inntektsmeldinger, periode);
        var inntektsmeldingerTilBeregning = filtrerForOpptjeningsaktiviteter(referanse, periode, inntektsmeldingBegrenset);
        return relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingerTilBeregning, periode);

    }

    private List<Inntektsmelding> filtrerForOpptjeningsaktiviteter(BehandlingReferanse referanse,
                                                                   DatoIntervallEntitet periode, Collection<Inntektsmelding> inntektsmeldingForPeriode) {

        var opptjeningAktiviteter = opptjeningInntektArbeidYtelseTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(
            referanse, Set.of(periode)
        ).get(periode);
        return filtrerForAktiviteter(inntektsmeldingForPeriode, opptjeningAktiviteter);
    }

    static List<Inntektsmelding> filtrerForAktiviteter(Collection<Inntektsmelding> inntektsmeldingForPeriode, List<OpptjeningAktivitetPeriode> opptjeningAktiviteter) {
        return inntektsmeldingForPeriode.stream().filter(im -> opptjeningAktiviteter.stream()
                .anyMatch(a -> erIkkeUnderkjent(a) && harLikArbeidsgiver(im, a) && gjelderForArbeidsforhold(im, a)))
            .toList();
    }

    private static boolean erIkkeUnderkjent(OpptjeningAktivitetPeriode a) {
        return a.getVurderingsStatus().equals(VurderingsStatus.GODKJENT) || a.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING);
    }

    private static boolean gjelderForArbeidsforhold(Inntektsmelding im, OpptjeningAktivitetPeriode a) {
        return im.getArbeidsforholdRef().gjelderFor(a.getOpptjeningsnøkkel().getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()));
    }

    private static boolean harLikArbeidsgiver(Inntektsmelding im, OpptjeningAktivitetPeriode a) {
        return Objects.equals(im.getArbeidsgiver().getArbeidsgiverOrgnr(), a.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.ORG_NUMMER)) &&
            Objects.equals(im.getArbeidsgiver().getArbeidsgiverAktørId(), a.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.AKTØR_ID));
    }

}
