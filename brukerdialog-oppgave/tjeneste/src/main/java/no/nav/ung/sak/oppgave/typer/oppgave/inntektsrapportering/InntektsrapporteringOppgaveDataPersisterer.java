package no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDTO;
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
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data) {
        var dto = (InntektsrapporteringOppgavetypeDataDTO) data;
        var entitet = new InntektsrapporteringOppgaveDataEntitet(
            oppgave,
            dto.fraOgMed(),
            dto.tilOgMed(),
            dto.gjelderDelerAvMåned()
        );
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
