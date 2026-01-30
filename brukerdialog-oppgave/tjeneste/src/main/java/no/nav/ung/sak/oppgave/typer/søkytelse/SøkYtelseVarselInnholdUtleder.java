package no.nav.ung.sak.oppgave.typer.søkytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveType;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.VarselInnholdUtleder;
import no.nav.ung.sak.oppgave.typer.kontrollerregisterinntekt.KontrollerRegisterInntektOppgaveData;

import java.time.Month;

@OppgaveTypeRef(OppgaveType.SØK_YTELSE)
@ApplicationScoped
public class SøkYtelseVarselInnholdUtleder implements VarselInnholdUtleder {

    private String ungdomsprogramytelsenDeltakerBaseUrl;

    @Inject
    public SøkYtelseVarselInnholdUtleder(
        @KonfigVerdi(value = "UNGDOMPROGRAMSYTELSEN_DELTAKER_BASE_URL") String ungdomsprogramytelsenDeltakerBaseUrl
    ) {
        this.ungdomsprogramytelsenDeltakerBaseUrl = ungdomsprogramytelsenDeltakerBaseUrl;
    }

    public SøkYtelseVarselInnholdUtleder() {
    }

    @Override
    public String utledVarselTekst(BrukerdialogOppgaveEntitet oppgave) {
        return "Søk om ungdomsprogramytelsen";
    }

    @Override
    public String utledVarselLenke(BrukerdialogOppgaveEntitet oppgave) {
        return ungdomsprogramytelsenDeltakerBaseUrl;
    }

}
