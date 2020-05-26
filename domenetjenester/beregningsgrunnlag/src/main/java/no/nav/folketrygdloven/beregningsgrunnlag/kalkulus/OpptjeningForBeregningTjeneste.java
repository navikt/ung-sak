package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;


import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;

/** Interface for å plugge inn ytelsespesifikk utregning av opptjeningaktiviteteter. */
public interface OpptjeningForBeregningTjeneste {

    OpptjeningAktiviteter hentEksaktOpptjeningForBeregning(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag);

}
