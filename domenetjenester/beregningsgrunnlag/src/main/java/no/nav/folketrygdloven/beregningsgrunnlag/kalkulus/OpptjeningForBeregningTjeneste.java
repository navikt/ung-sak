package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Optional;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningFilter;

/** Interface for å plugge inn ytelsespesifikk utregning av opptjeningaktiviteteter. */
public interface OpptjeningForBeregningTjeneste {

    OpptjeningAktiviteter hentEksaktOpptjeningForBeregning(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag);

    default Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        OppgittOpptjeningFilter filter = new OppgittOpptjeningFilter(iayGrunnlag.getOppgittOpptjening(), iayGrunnlag.getOverstyrtOppgittOpptjening());
        return filter.getOppgittOpptjeningStandard();
    }
}
