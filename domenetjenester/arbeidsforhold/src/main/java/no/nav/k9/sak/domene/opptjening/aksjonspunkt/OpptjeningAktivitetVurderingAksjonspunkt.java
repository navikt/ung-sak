package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.Set;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

public class OpptjeningAktivitetVurderingAksjonspunkt implements OpptjeningAktivitetVurdering {

    private AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening;
    private AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening;

    public OpptjeningAktivitetVurderingAksjonspunkt(AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening, AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening) {
        this.vurderOppgittOpptjening = vurderOppgittOpptjening;
        this.vurderBekreftetOpptjening = vurderBekreftetOpptjening;
    }

    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse ref,
                                         Yrkesaktivitet registerAktivitet,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         boolean harVærtSaksbehandlet,
                                         DatoIntervallEntitet opptjeningPeriode,
                                         LocalDate skjæringstidspunkt,
                                         Set<Inntektsmelding> inntektsmeldinger) {

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            return vurderAnnenOpptjening(overstyrtAktivitet, harVærtSaksbehandlet);
        } else if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            return vurderNæring(ref, ref.getAktørId(), overstyrtAktivitet, iayGrunnlag, harVærtSaksbehandlet, opptjeningPeriode, skjæringstidspunkt);
        } else if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            return vurderArbeid(filter, registerAktivitet, overstyrtAktivitet, harVærtSaksbehandlet, opptjeningPeriode, inntektsmeldinger);
        }
        return VurderingsStatus.GODKJENT;
    }

    /**
     * @param registerAktivitet    aktiviteten
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @param opptjeningPeriode    opptjeningsperioder
     * @param inntektsmeldinger    inntektsmeldinger
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderArbeid(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet,
                                          boolean harVærtSaksbehandlet,
                                          DatoIntervallEntitet opptjeningPeriode,
                                          Set<Inntektsmelding> inntektsmeldinger) {
        if (vurderBekreftetOpptjening.girAksjonspunktForArbeidsforhold(filter, registerAktivitet, overstyrtAktivitet, opptjeningPeriode, inntektsmeldinger)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.GODKJENT;
            }
            if (harVærtSaksbehandlet) {
                return VurderingsStatus.UNDERKJENT;
            }
            return VurderingsStatus.TIL_VURDERING;
        }
        return VurderingsStatus.GODKJENT;
    }

    /**
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderAnnenOpptjening(Yrkesaktivitet overstyrtAktivitet, boolean harVærtSaksbehandlet) {
        if (overstyrtAktivitet != null) {
            return VurderingsStatus.GODKJENT;
        }
        if (harVærtSaksbehandlet) {
            return VurderingsStatus.UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    /**
     *
     * @param ref
     * @param aktørId              aktør
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @param opptjeningPeriode    opptjeningsperiode
     * @param skjæringstidspunkt
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderNæring(BehandlingReferanse ref, AktørId aktørId, Yrkesaktivitet overstyrtAktivitet, InntektArbeidYtelseGrunnlag iayGrunnlag, boolean harVærtSaksbehandlet, DatoIntervallEntitet opptjeningPeriode, LocalDate skjæringstidspunkt) {
        if (vurderOppgittOpptjening.girAksjonspunktForOppgittNæring(ref.getBehandlingId(), aktørId, iayGrunnlag, opptjeningPeriode, skjæringstidspunkt)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.GODKJENT;
            }
            if (harVærtSaksbehandlet) {
                return VurderingsStatus.UNDERKJENT;
            }
            return VurderingsStatus.TIL_VURDERING;
        }
        return VurderingsStatus.GODKJENT;
    }

    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        return vurderStatus(input.getType(), input.getBehandlingReferanse(),
            input.getRegisterAktivitet(),
            input.getOverstyrtAktivitet(),
            input.getIayGrunnlag(),
            input.getHarVærtSaksbehandlet(),
            input.getOpptjeningPeriode(),
            input.getSkjæringstidspunkt(),
            input.getInntektsmeldinger());
    }
}
