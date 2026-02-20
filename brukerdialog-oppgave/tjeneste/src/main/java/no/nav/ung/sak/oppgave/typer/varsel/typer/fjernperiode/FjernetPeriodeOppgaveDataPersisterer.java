package no.nav.ung.sak.oppgave.typer.varsel.typer.fjernperiode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.fjernperiode.FjernetPeriodeDataDTO;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveDataPersisterer;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_FJERNET_PERIODE)
public class FjernetPeriodeOppgaveDataPersisterer implements OppgaveDataPersisterer {

    private EntityManager entityManager;

    protected FjernetPeriodeOppgaveDataPersisterer() {
        // CDI proxy
    }

    @Inject
    public FjernetPeriodeOppgaveDataPersisterer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data) {
        var dto = (FjernetPeriodeDataDTO) data;
        var entitet = new FjernetPeriodeOppgaveDataEntitet(
            oppgave,
            dto.forrigeStartdato(),
            dto.forrigeSluttdato()
        );
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
