package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
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

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         boolean harVærtSaksbehandlet,
                                         DatoIntervallEntitet opptjeningPeriode) {
        return vurderStatus(type, behandlingReferanse, null, overstyrtAktivitet, iayGrunnlag, harVærtSaksbehandlet, opptjeningPeriode);
    }

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse,
                                         Yrkesaktivitet registerAktivitet,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         boolean harVærtSaksbehandlet,
                                         DatoIntervallEntitet opptjeningPeriode) {

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            return vurderAnnenOpptjening(overstyrtAktivitet, harVærtSaksbehandlet);
        } else if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            return vurderNæring(behandlingReferanse.getAktørId(), overstyrtAktivitet, iayGrunnlag, harVærtSaksbehandlet, opptjeningPeriode);
        } else if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            return vurderArbeid(filter, registerAktivitet, overstyrtAktivitet, harVærtSaksbehandlet, opptjeningPeriode);
        }
        return VurderingsStatus.GODKJENT;
    }

    /**
     * @param registerAktivitet    aktiviteten
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @param opptjeningPeriode
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderArbeid(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet, boolean harVærtSaksbehandlet, DatoIntervallEntitet opptjeningPeriode) {
        if (vurderBekreftetOpptjening.girAksjonspunktForArbeidsforhold(filter, registerAktivitet, overstyrtAktivitet, opptjeningPeriode)) {
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
     * @param aktørId
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @param opptjeningPeriode
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderNæring(AktørId aktørId, Yrkesaktivitet overstyrtAktivitet, InntektArbeidYtelseGrunnlag iayGrunnlag, boolean harVærtSaksbehandlet, DatoIntervallEntitet opptjeningPeriode) {
        if (vurderOppgittOpptjening.girAksjonspunktForOppgittNæring(aktørId, iayGrunnlag, opptjeningPeriode)) {
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
}
