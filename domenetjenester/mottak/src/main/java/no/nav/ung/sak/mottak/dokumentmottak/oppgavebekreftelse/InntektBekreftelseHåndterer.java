package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_AVVIK_REGISTERINNTEKT)
public class InntektBekreftelseHåndterer implements BekreftelseHåndterer {


    @Inject
    public InntektBekreftelseHåndterer() {
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold bekreftelse) {

    }
}
