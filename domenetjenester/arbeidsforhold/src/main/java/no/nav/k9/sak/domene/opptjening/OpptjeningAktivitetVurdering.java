package no.nav.k9.sak.domene.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;

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
