package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.Set;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

public class OpptjeningAktivitetVurderingVilkår implements OpptjeningAktivitetVurdering {

    private AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening;
    private AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening;

    public OpptjeningAktivitetVurderingVilkår(AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening, AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening) {
        this.vurderOppgittOpptjening = vurderOppgittOpptjening;
        this.vurderBekreftetOpptjening = vurderBekreftetOpptjening;
    }

    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse,
                                         Yrkesaktivitet registerAktivitet,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         DatoIntervallEntitet opptjeningPeriode,
                                         Set<Inntektsmelding> inntektsmeldinger) {

        if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            return vurderNæring(iayGrunnlag, overstyrtAktivitet, opptjeningPeriode, behandlingReferanse.getAktørId());
        } else if (OpptjeningAktivitetType.FRILANS.equals(type)) {
            return vurderFrilans(iayGrunnlag, overstyrtAktivitet);
        } else if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
            return vurderArbeid(filter, registerAktivitet, overstyrtAktivitet, opptjeningPeriode, inntektsmeldinger);
        } else if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            return vurderAnnenOpptjening(overstyrtAktivitet);
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    private VurderingsStatus vurderArbeid(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet, DatoIntervallEntitet opptjeningPeriode,
                                          Set<Inntektsmelding> inntektsmeldinger) {
        if (vurderBekreftetOpptjening.girAksjonspunktForArbeidsforhold(filter, registerAktivitet, overstyrtAktivitet, opptjeningPeriode, inntektsmeldinger)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
            }
            return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    private VurderingsStatus vurderAnnenOpptjening(Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet != null) {
            return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
        }
        return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
    }

    private VurderingsStatus vurderFrilans(InntektArbeidYtelseGrunnlag iayGrunnlag, Yrkesaktivitet overstyrtAktivitet) {
        //avklart med funksjonell at når frilans arbeidsforhold er oppgitt i søknad, så er det automatisk godkjent som opptjeningsaktivitet
        boolean harSøkt = iayGrunnlag.getOppgittOpptjening().flatMap(OppgittOpptjening::getFrilans).isPresent();
        return harSøkt || overstyrtAktivitet != null
            ? VurderingsStatus.FERDIG_VURDERT_GODKJENT
            : VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
    }

    private VurderingsStatus vurderNæring(InntektArbeidYtelseGrunnlag iayGrunnlag, Yrkesaktivitet overstyrtAktivitet, DatoIntervallEntitet opptjeningPeriode, AktørId aktørId) {
        boolean harSøkt = iayGrunnlag.getOppgittOpptjening().map(o -> !o.getEgenNæring().isEmpty()).orElse(false);
        if (harSøkt) {
            //avklart med funksjonell at når egen næring er oppgitt i søknad, så er det automatisk godkjent som opptjeningsaktivitet
            return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
        }

        if (vurderOppgittOpptjening.girAksjonspunktForOppgittNæring(aktørId, iayGrunnlag, opptjeningPeriode)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
            }
            return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        return vurderStatus(input.getType(), input.getBehandlingReferanse(),
            input.getRegisterAktivitet(),
            input.getOverstyrtAktivitet(),
            input.getIayGrunnlag(),
            input.getOpptjeningPeriode(),
            input.getInntektsmeldinger());
    }
}
