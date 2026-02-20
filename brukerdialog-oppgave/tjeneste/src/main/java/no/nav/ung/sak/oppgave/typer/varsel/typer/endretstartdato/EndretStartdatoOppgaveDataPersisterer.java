package no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDTO;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveDataPersisterer;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_STARTDATO)
public class EndretStartdatoOppgaveDataPersisterer implements OppgaveDataPersisterer {

    private EntityManager entityManager;

    protected EndretStartdatoOppgaveDataPersisterer() {
        // CDI proxy
    }

    @Inject
    public EndretStartdatoOppgaveDataPersisterer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data) {
        var dto = (EndretStartdatoDataDTO) data;
        var entitet = new EndretStartdatoOppgaveDataEntitet(
            oppgave,
            dto.nyStartdato(),
            dto.forrigeStartdato()
        );
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
