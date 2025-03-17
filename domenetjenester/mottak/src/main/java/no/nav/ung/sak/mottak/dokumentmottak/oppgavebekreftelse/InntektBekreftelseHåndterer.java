package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.Dependent;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_AVVIK_REGISTERINNTEKT)
public class InntektBekreftelseHåndterer implements BekreftelseHåndterer {


    @Override
    public void håndter(OppgaveBekreftelseInnhold bekreftelse) {

    }
}
