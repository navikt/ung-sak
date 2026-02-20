package no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDTO;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveDataPersisterer;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_SLUTTDATO)
public class EndretSluttdatoOppgaveDataPersisterer implements OppgaveDataPersisterer {

    private EntityManager entityManager;

    protected EndretSluttdatoOppgaveDataPersisterer() {
        // CDI proxy
    }

    @Inject
    public EndretSluttdatoOppgaveDataPersisterer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data) {
        var dto = (EndretSluttdatoDataDTO) data;
        var entitet = new EndretSluttdatoOppgaveDataEntitet(
            oppgave,
            dto.nySluttdato(),
            dto.forrigeSluttdato()
        );
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
