package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.søknad.BrukerdialogSøknadEntitet;
import no.nav.ung.sak.oppgave.varsel.BrukerdialogVarselEntitet;

import java.util.ArrayList;
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

    public List<BrukerdialogVarselEntitet> hentAlleVarslerForAktør(AktørId aktørId) {
        TypedQuery<BrukerdialogVarselEntitet> query = entityManager.createQuery(
            "SELECT v FROM BrukerdialogVarsel v WHERE v.aktørId = :aktørId ORDER BY v.opprettetTidspunkt DESC",
            BrukerdialogVarselEntitet.class
        );
        query.setParameter("aktørId", aktørId);
        return query.getResultList();
    }

    public List<BrukerdialogSøknadEntitet> hentAlleSøknaderForAktør(AktørId aktørId) {
        TypedQuery<BrukerdialogSøknadEntitet> query = entityManager.createQuery(
            "SELECT s FROM BrukerdialogSøknad s WHERE s.aktørId = :aktørId ORDER BY s.opprettetTidspunkt DESC",
            BrukerdialogSøknadEntitet.class
        );
        query.setParameter("aktørId", aktørId);
        return query.getResultList();
    }

    public List<BrukerdialogOppgaveEntitet> hentAlleOppgaverForAktør(AktørId aktørId) {
        List<BrukerdialogOppgaveEntitet> oppgaver = new ArrayList<>();
        oppgaver.addAll(hentAlleVarslerForAktør(aktørId));
        oppgaver.addAll(hentAlleSøknaderForAktør(aktørId));
        return oppgaver;
    }

    public Optional<BrukerdialogVarselEntitet> hentVarselForOppgavereferanse(UUID oppgavereferanse) {
        TypedQuery<BrukerdialogVarselEntitet> query = entityManager.createQuery(
            "SELECT v FROM BrukerdialogVarsel v WHERE v.oppgavereferanse = :oppgavereferanse",
            BrukerdialogVarselEntitet.class
        );
        query.setParameter("oppgavereferanse", oppgavereferanse);
        return query.getResultList().stream().findFirst();
    }

    public Optional<BrukerdialogSøknadEntitet> hentSøknadForOppgavereferanse(UUID oppgavereferanse) {
        TypedQuery<BrukerdialogSøknadEntitet> query = entityManager.createQuery(
            "SELECT s FROM BrukerdialogSøknad s WHERE s.oppgavereferanse = :oppgavereferanse",
            BrukerdialogSøknadEntitet.class
        );
        query.setParameter("oppgavereferanse", oppgavereferanse);
        return query.getResultList().stream().findFirst();
    }

    public Optional<BrukerdialogOppgaveEntitet> hentOppgaveForOppgavereferanse(UUID oppgavereferanse) {
        Optional<BrukerdialogVarselEntitet> varsel = hentVarselForOppgavereferanse(oppgavereferanse);
        if (varsel.isPresent()) {
            return Optional.of(varsel.get());
        }
        return hentSøknadForOppgavereferanse(oppgavereferanse).map(s -> s);
    }

    public void persister(BrukerdialogVarselEntitet varsel) {
        entityManager.persist(varsel);
        entityManager.flush();
    }

    public void persister(BrukerdialogSøknadEntitet søknad) {
        entityManager.persist(søknad);
        entityManager.flush();
    }

    public BrukerdialogVarselEntitet oppdater(BrukerdialogVarselEntitet varsel) {
        return entityManager.merge(varsel);
    }

    public BrukerdialogSøknadEntitet oppdater(BrukerdialogSøknadEntitet søknad) {
        return entityManager.merge(søknad);
    }

    public BrukerdialogOppgaveEntitet oppdater(BrukerdialogOppgaveEntitet oppgave) {
        if (oppgave instanceof BrukerdialogVarselEntitet) {
            return entityManager.merge((BrukerdialogVarselEntitet) oppgave);
        } else if (oppgave instanceof BrukerdialogSøknadEntitet) {
            return entityManager.merge((BrukerdialogSøknadEntitet) oppgave);
        }
        throw new IllegalArgumentException("Ukjent oppgavetype: " + oppgave.getClass().getName());
    }
}

