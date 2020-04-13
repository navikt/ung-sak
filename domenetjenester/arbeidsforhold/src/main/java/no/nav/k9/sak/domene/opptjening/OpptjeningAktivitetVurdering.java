package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface OpptjeningAktivitetVurdering {

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet, DatoIntervallEntitet opptjeningPeriode);

    VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                  BehandlingReferanse behandlingReferanse,
                                  Yrkesaktivitet registerAktivitet,
                                  Yrkesaktivitet overstyrtAktivitet,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  boolean harVærtSaksbehandlet, DatoIntervallEntitet opptjeningPeriode);
}
