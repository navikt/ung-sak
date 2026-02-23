package no.nav.ung.sak.oppgave.typer.varsel.typer.fjernperiode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.fjernperiode.FjernetPeriodeDataDto;
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
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDto data) {
        var dto = (FjernetPeriodeDataDto) data;
        var entitet = new FjernetPeriodeOppgaveDataEntitet(
            dto.forrigeStartdato(),
            dto.forrigeSluttdato()
        );
        oppgave.setOppgaveData(entitet);
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
