package no.nav.foreldrepenger.domene.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public interface OpptjeningAktivitetVurdering {

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet);

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Yrkesaktivitet registerAktivitet,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet);
}
