package no.nav.ung.sak.oppgave;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;

public interface OppgavelInnholdUtleder {

    public static OppgavelInnholdUtleder finnUtleder(Instance<OppgavelInnholdUtleder> utledere, OppgaveType oppgaveType) {
        return OppgaveTypeRef.Lookup.find(utledere, oppgaveType)
            .orElseThrow(() -> new IllegalArgumentException("Finner ingen varsel innhold utleder for oppgavetype: " + oppgaveType));
    }

    String utledVarselTekst(BrukerdialogOppgaveEntitet oppgave);
    String utledVarselLenke(BrukerdialogOppgaveEntitet oppgave);

}
