package no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveDataPersisterer;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.RAPPORTER_INNTEKT)
public class InntektsrapporteringOppgaveDataPersisterer implements OppgaveDataPersisterer {

    private EntityManager entityManager;

    protected InntektsrapporteringOppgaveDataPersisterer() {
        // CDI proxy
    }

    @Inject
    public InntektsrapporteringOppgaveDataPersisterer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDto data) {
        var dto = (InntektsrapporteringOppgavetypeDataDto) data;
        var entitet = new InntektsrapporteringOppgaveDataEntitet(
            dto.fraOgMed(),
            dto.tilOgMed(),
            dto.gjelderDelerAvMåned()
        );
        oppgave.setOppgaveData(entitet);
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
