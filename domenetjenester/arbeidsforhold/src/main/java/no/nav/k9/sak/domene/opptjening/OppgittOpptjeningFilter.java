package no.nav.k9.sak.domene.opptjening;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface OppgittOpptjeningFilter {

    default Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        return iayGrunnlag.getOppgittOpptjening();
    }

    default Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        return iayGrunnlag.getOppgittOpptjening();
    }
}
