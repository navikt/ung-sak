package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;

@ApplicationScoped
public class BrukerdialogOppgaveRepository {

    private EntityManager entityManager;
    private OppgaveDataEntitetMapper oppgaveDataEntitetMapper;

    public BrukerdialogOppgaveRepository() {
        // CDI proxy
    }

    @Inject
    public BrukerdialogOppgaveRepository(EntityManager entityManager,
                                          OppgaveDataEntitetMapper oppgaveDataEntitetMapper) {
        this.entityManager = entityManager;
        this.oppgaveDataEntitetMapper = oppgaveDataEntitetMapper;
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

    /**
     * Mapper {@link OppgavetypeDataDTO} til riktig JPA-entitet og persisterer den.
     * Må kalles etter at {@code oppgave} allerede er persistert slik at FK-referansen er gyldig.
     *
     * @param oppgave oppgaven dataen tilhører
     * @param data    oppgavetypedata som skal lagres
     */
    public void persisterOppgaveData(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data) {
        oppgaveDataEntitetMapper.persister(oppgave, data);
    }

    public BrukerdialogOppgaveEntitet oppdater(BrukerdialogOppgaveEntitet oppgave) {
        BrukerdialogOppgaveEntitet merged = entityManager.merge(oppgave);
        persister(merged);
        return merged;
    }
}

