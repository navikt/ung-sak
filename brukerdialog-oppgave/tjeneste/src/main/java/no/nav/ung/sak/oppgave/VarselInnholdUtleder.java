package no.nav.ung.sak.oppgave;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.sak.oppgave.kontrakt.OppgaveType;

public interface VarselInnholdUtleder {

    public static VarselInnholdUtleder finnUtleder(Instance<VarselInnholdUtleder> utledere, OppgaveType oppgaveType) {
        return OppgaveTypeRef.Lookup.find(utledere, oppgaveType)
            .orElseThrow(() -> new IllegalArgumentException("Finner ingen varsel innhold utleder for oppgavetype: " + oppgaveType));
    }

    String utledVarselTekst(BrukerdialogOppgaveEntitet oppgave);
    String utledVarselLenke(BrukerdialogOppgaveEntitet oppgave);

}
