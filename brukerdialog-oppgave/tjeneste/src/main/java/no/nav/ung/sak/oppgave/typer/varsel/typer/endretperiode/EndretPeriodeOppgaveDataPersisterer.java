package no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveDataPersisterer;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_PERIODE)
public class EndretPeriodeOppgaveDataPersisterer implements OppgaveDataPersisterer {

    private EntityManager entityManager;

    protected EndretPeriodeOppgaveDataPersisterer() {
        // CDI proxy
    }

    @Inject
    public EndretPeriodeOppgaveDataPersisterer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDto data) {
        var dto = (EndretPeriodeDataDto) data;
        var entitet = new EndretPeriodeOppgaveDataEntitet(
            oppgave,
            dto.nyPeriode() != null ? dto.nyPeriode().getFomDato() : null,
            dto.nyPeriode() != null ? dto.nyPeriode().getTomDato() : null,
            dto.forrigePeriode() != null ? dto.forrigePeriode().getFomDato() : null,
            dto.forrigePeriode() != null ? dto.forrigePeriode().getTomDato() : null,
            dto.endringer()
        );
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
