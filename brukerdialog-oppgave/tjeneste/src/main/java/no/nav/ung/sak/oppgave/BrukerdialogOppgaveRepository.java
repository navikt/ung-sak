package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.kontrakt.OppgaveStatus;
import no.nav.ung.sak.oppgave.kontrakt.OppgaveType;

import java.time.LocalDateTime;
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

    public List<BrukerdialogOppgaveEntitet> hentOppgaveForType(OppgaveType type, OppgaveStatus status, AktørId aktørId) {
        TypedQuery<BrukerdialogOppgaveEntitet> query = entityManager.createQuery(
            "SELECT o FROM BrukerdialogOppgave o WHERE o.oppgaveType = :type AND o.status = :status AND o.aktørId = :aktørId",
            BrukerdialogOppgaveEntitet.class
        );
        query.setParameter("type", type);
        query.setParameter("status", status);
        query.setParameter("aktørId", aktørId);
        return query.getResultList();
    }

    public Optional<BrukerdialogOppgaveEntitet> hentOppgaveForOppgavereferanse(UUID oppgavereferanse) {
        TypedQuery<BrukerdialogOppgaveEntitet> query = entityManager.createQuery(
            "SELECT o FROM BrukerdialogOppgave o WHERE o.oppgavereferanse = :oppgavereferanse",
            BrukerdialogOppgaveEntitet.class
        );
        query.setParameter("oppgavereferanse", oppgavereferanse);
        return query.getResultList().stream().findFirst();
    }

    public BrukerdialogOppgaveEntitet endreFrist(UUID oppgaveReferanse, AktørId aktørId, LocalDateTime nyFrist) {
        var brukerdialogOppgaveEntitet = hentOppgaveForOppgavereferanse(oppgaveReferanse, aktørId).orElseThrow(() ->
            new IllegalArgumentException("Fant ingen oppgave for oppgavereferanse " + oppgaveReferanse)
        );
        brukerdialogOppgaveEntitet.setFristTid(nyFrist);
        return oppdater(brukerdialogOppgaveEntitet);
    }

    public BrukerdialogOppgaveEntitet lukkOppgave(BrukerdialogOppgaveEntitet oppgave) {
        oppgave.setStatus(OppgaveStatus.LUKKET);
        oppgave.setLukketDato(LocalDateTime.now());
        return oppdater(oppgave);
    }

    public BrukerdialogOppgaveEntitet åpneOppgave(BrukerdialogOppgaveEntitet oppgave) {
        oppgave.setÅpnetDato(LocalDateTime.now());
        return oppdater(oppgave);
    }


    public void persister(BrukerdialogOppgaveEntitet oppgave) {
        entityManager.persist(oppgave);
        entityManager.flush();
    }

    public BrukerdialogOppgaveEntitet oppdater(BrukerdialogOppgaveEntitet oppgave) {
        BrukerdialogOppgaveEntitet merged = entityManager.merge(oppgave);
        persister(merged);
        return merged;
    }
}

