package no.nav.ung.sak.oppgave.typer.oppgave.søkytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveDataPersisterer;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.SØK_YTELSE)
public class SøkYtelseOppgaveDataPersisterer implements OppgaveDataPersisterer {

    private EntityManager entityManager;

    protected SøkYtelseOppgaveDataPersisterer() {
        // CDI proxy
    }

    @Inject
    public SøkYtelseOppgaveDataPersisterer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDto data) {
        var dto = (SøkYtelseOppgavetypeDataDto) data;
        var entitet = new SøkYtelseOppgaveDataEntitet(
            dto.fomDato()
        );
        oppgave.setOppgaveData(entitet);
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
