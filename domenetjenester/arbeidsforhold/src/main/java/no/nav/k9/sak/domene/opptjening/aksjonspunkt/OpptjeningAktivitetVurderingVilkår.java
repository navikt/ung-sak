package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
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
                                         SakInntektsmeldinger inntektsmeldinger) {
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            return vurderAnnenOpptjening(overstyrtAktivitet);
        } else if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            return vurderNæring(iayGrunnlag, overstyrtAktivitet, opptjeningPeriode, behandlingReferanse.getAktørId());
        } else if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
            return vurderArbeid(filter, registerAktivitet, overstyrtAktivitet, opptjeningPeriode, inntektsmeldinger);
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    private VurderingsStatus vurderArbeid(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet, DatoIntervallEntitet opptjeningPeriode, SakInntektsmeldinger inntektsmeldinger) {
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

    private VurderingsStatus vurderNæring(InntektArbeidYtelseGrunnlag iayg, Yrkesaktivitet overstyrtAktivitet, DatoIntervallEntitet opptjeningPeriode, AktørId aktørId) {
        if (vurderOppgittOpptjening.girAksjonspunktForOppgittNæring(aktørId, iayg, opptjeningPeriode)) {
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
