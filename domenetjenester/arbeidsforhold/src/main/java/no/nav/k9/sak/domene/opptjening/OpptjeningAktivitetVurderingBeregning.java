package no.nav.k9.sak.domene.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;

public class OpptjeningAktivitetVurderingBeregning implements OpptjeningAktivitetVurdering {

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type, BehandlingReferanse behandlingReferanse, Yrkesaktivitet overstyrtAktivitet, InntektArbeidYtelseGrunnlag iayGrunnlag, boolean harVærtSaksbehandlet) {
        return VurderingsStatus.GODKJENT;
    }

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse,
                                         Yrkesaktivitet registerAktivitet,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         boolean harVærtSaksbehandlet) {
        return VurderingsStatus.GODKJENT;
    }
}
