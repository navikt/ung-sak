package no.nav.k9.sak.domene.opptjening;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultOppgittOpptjeningTjeneste implements OppgittOpptjeningTjeneste {

    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        return iayGrunnlag.getOppgittOpptjening();
    }

}
