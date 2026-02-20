package no.nav.ung.sak.oppgave.typer.oppgave.søkytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDTO;
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
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data) {
        var dto = (SøkYtelseOppgavetypeDataDTO) data;
        var entitet = new SøkYtelseOppgaveDataEntitet(
            oppgave,
            dto.fomDato()
        );
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
