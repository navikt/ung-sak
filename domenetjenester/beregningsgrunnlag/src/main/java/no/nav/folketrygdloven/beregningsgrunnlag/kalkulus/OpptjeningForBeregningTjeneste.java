package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;


import java.util.Optional;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;

/** Interface for å plugge inn ytelsespesifikk utregning av opptjeningaktiviteteter. */
public interface OpptjeningForBeregningTjeneste {

    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag);

    OpptjeningAktiviteter hentEksaktOpptjeningForBeregning(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag);

}
