package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import no.nav.ung.sak.felles.typer.AktørId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BrukerdialogOppgaveRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public BrukerdialogOppgaveRepository() {
        // CDI proxy
    }

    public List<BrukerdialogOppgaveEntitet> hentAlleOppgaverForAktør(AktørId aktørId) {
        TypedQuery<BrukerdialogOppgaveEntitet> query = entityManager.createQuery(
            "SELECT o FROM BrukerdialogOppgave o WHERE o.aktørId = :aktørId ORDER BY o.opprettetTidspunkt DESC",
            BrukerdialogOppgaveEntitet.class
        );
        query.setParameter("aktørId", aktørId);
        return query.getResultList();
    }

    public Optional<BrukerdialogOppgaveEntitet> hentOppgaveForOppgavereferanse(UUID oppgavereferanse, AktørId aktørId) {
        TypedQuery<BrukerdialogOppgaveEntitet> query = entityManager.createQuery(
            "SELECT o FROM BrukerdialogOppgave o WHERE o.oppgavereferanse = :oppgavereferanse AND o.aktørId = :aktørId",
            BrukerdialogOppgaveEntitet.class
        );
        query.setParameter("oppgavereferanse", oppgavereferanse);
        query.setParameter("aktørId", aktørId);
        return query.getResultList().stream().findFirst();
    }

    public Optional<BrukerdialogOppgaveEntitet> hentOppgaveForOppgavereferanse(UUID oppgavereferanse) {
        TypedQuery<BrukerdialogOppgaveEntitet> query = entityManager.createQuery(
            "SELECT o FROM BrukerdialogOppgave o WHERE o.oppgavereferanse = :oppgavereferanse",
            BrukerdialogOppgaveEntitet.class
        );
        query.setParameter("oppgavereferanse", oppgavereferanse);
        return query.getResultList().stream().findFirst();
    }

    public void persister(BrukerdialogOppgaveEntitet oppgave) {
        entityManager.persist(oppgave);
        entityManager.flush();
    }

    public BrukerdialogOppgaveEntitet oppdater(BrukerdialogOppgaveEntitet oppgave) {
        return entityManager.merge(oppgave);
    }
}

