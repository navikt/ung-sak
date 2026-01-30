package no.nav.ung.sak.oppgave.typer.endretstartdato;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.kontrakt.OppgaveType;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.VarselInnholdUtleder;

@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_STARTDATO)
@ApplicationScoped
public class EndretStartdatoVarselInnholdUtleder implements VarselInnholdUtleder {

    private String ungdomsprogramytelsenDeltakerBaseUrl;

    @Inject
    public EndretStartdatoVarselInnholdUtleder(
        @KonfigVerdi(value = "UNGDOMPROGRAMSYTELSEN_DELTAKER_BASE_URL") String ungdomsprogramytelsenDeltakerBaseUrl
    ) {
        this.ungdomsprogramytelsenDeltakerBaseUrl = ungdomsprogramytelsenDeltakerBaseUrl;
    }

    public EndretStartdatoVarselInnholdUtleder() {
    }

    @Override
    public String utledVarselTekst(BrukerdialogOppgaveEntitet oppgave) {
        return "Se og gi tilbakemelding på endret startdato i ungdomsprogrammet";
    }

    @Override
    public String utledVarselLenke(BrukerdialogOppgaveEntitet oppgave) {
        return ungdomsprogramytelsenDeltakerBaseUrl + "/oppgave" + oppgave.getOppgavereferanse();
    }

}
