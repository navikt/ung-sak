package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;


import java.util.Optional;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/** Interface for å plugge inn ytelsespesifikk utregning av opptjeningaktiviteteter. */
public interface OpptjeningForBeregningTjeneste {

    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag);

    Optional<OpptjeningAktiviteter> hentEksaktOpptjeningForBeregning(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode);

}
