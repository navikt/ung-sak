package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.DeaktiverMinSideVarselTask;
import no.nav.ung.sak.PubliserMinSideVarselTask;

import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class OppgaveLivssyklusTjeneste {

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private BrukerdialogOppgaveRepository brukerdialogOppgaveRepository;
    private Instance<VarselInnholdUtleder> varselInnholdUtledere;

    public OppgaveLivssyklusTjeneste() {
    }

    @Inject
    public OppgaveLivssyklusTjeneste(ProsessTaskTjeneste prosessTaskTjeneste, BrukerdialogOppgaveRepository brukerdialogOppgaveRepository,
                                     @Any Instance<VarselInnholdUtleder> varselInnholdUtledere) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.brukerdialogOppgaveRepository = brukerdialogOppgaveRepository;
        this.varselInnholdUtledere = varselInnholdUtledere;
    }

    /**
     * Løser en oppgave og oppdaterer status.
     * Deaktiverer varsel på Min Side og setter oppgaven til LØST status med løst-dato.
     *
     * @param oppgaveEntitet Oppgaven som skal løses
     * @return
     */
    public BrukerdialogOppgaveEntitet løsOppgave(BrukerdialogOppgaveEntitet oppgaveEntitet) {
        opprettTaskForDeaktiveringAvVarsel(oppgaveEntitet);
        oppgaveEntitet.setStatus(OppgaveStatus.LØST);
        oppgaveEntitet.setLøstDato(java.time.LocalDateTime.now());
        brukerdialogOppgaveRepository.oppdater(oppgaveEntitet);
        return oppgaveEntitet;
    }

    /**
     * Markerer en oppgave som utløpt.
     * Deaktiverer varsel på Min Side og setter oppgaven til UTLØPT status.
     * Brukes når fristen for oppgaven har gått ut.
     *
     * @param oppgaveEntitet Oppgaven som skal markeres som utløpt
     */
    public void utløpOppgave(BrukerdialogOppgaveEntitet oppgaveEntitet) {
        opprettTaskForDeaktiveringAvVarsel(oppgaveEntitet);
        oppgaveEntitet.setStatus(OppgaveStatus.UTLØPT);
        brukerdialogOppgaveRepository.oppdater(oppgaveEntitet);
    }

    /**
     * Avbryter en oppgave.
     * Deaktiverer varsel på Min Side og setter oppgaven til AVBRUTT status.
     * Brukes når oppgaven ikke lenger er relevant eller skal kanselleres.
     *
     * @param oppgaveEntitet Oppgaven som skal avbryttes
     */
    public void avbrytOppgave(BrukerdialogOppgaveEntitet oppgaveEntitet) {
        opprettTaskForDeaktiveringAvVarsel(oppgaveEntitet);
        oppgaveEntitet.setStatus(OppgaveStatus.AVBRUTT);
        brukerdialogOppgaveRepository.oppdater(oppgaveEntitet);
    }

    /**
     * Persisterer oppgave og publiserer varsel til Min Side.
     *
     * @param oppgaveEntitet Oppgave som skal opprettes og publiseres.
     */
    public void opprettOppgave(BrukerdialogOppgaveEntitet oppgaveEntitet) {
        if (oppgaveEntitet.getId() != null) {
            throw new IllegalArgumentException("Oppgave er allerede persistert med id: " + oppgaveEntitet.getId());
        }
        opprettTaskForPubliseringAvVarsel(oppgaveEntitet);
        oppgaveEntitet.setStatus(OppgaveStatus.ULØST);
        brukerdialogOppgaveRepository.persister(oppgaveEntitet);
    }

    private void opprettTaskForPubliseringAvVarsel(BrukerdialogOppgaveEntitet oppgaveEntitet) {
        VarselInnholdUtleder varselInnholdUtleder = VarselInnholdUtleder.finnUtleder(varselInnholdUtledere, oppgaveEntitet.getOppgaveType());
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(PubliserMinSideVarselTask.class);
        prosessTaskData.setProperty(PubliserMinSideVarselTask.OPPGAVE_REFERANSE, oppgaveEntitet.getOppgavereferanse().toString());
        prosessTaskData.setProperty(ProsessTaskData.AKTØR_ID, oppgaveEntitet.getAktørId().getId());
        prosessTaskData.setProperty(PubliserMinSideVarselTask.FRIST, oppgaveEntitet.getFristTid().format(DateTimeFormatter.ISO_DATE_TIME));
        prosessTaskData.setProperty(PubliserMinSideVarselTask.VARSEL_TEKST, varselInnholdUtleder.utledVarselTekst(oppgaveEntitet));
        prosessTaskData.setProperty(PubliserMinSideVarselTask.VARSEL_LENKE, varselInnholdUtleder.utledVarselLenke(oppgaveEntitet));
        prosessTaskTjeneste.lagre(prosessTaskData);
    }

    private void opprettTaskForDeaktiveringAvVarsel(BrukerdialogOppgaveEntitet oppgaveEntitet) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(DeaktiverMinSideVarselTask.class);
        prosessTaskData.setProperty(DeaktiverMinSideVarselTask.OPPGAVE_REFERANSE, oppgaveEntitet.getOppgavereferanse().toString());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }


}
