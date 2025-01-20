package no.nav.ung.fordel.repo.journalpost;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.jpa.SpecHints;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

@Dependent
public class JournalpostRepository {

    private EntityManager entityManager;

    protected JournalpostRepository() {
    }

    @Inject
    public JournalpostRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    public List<JournalpostInnsendingEntitet> markerOgHentJournalposterKlarForInnsending(FagsakYtelseType ytelseType, Saksnummer saksnummer) {
        return markerOgHentJournalposterKlarForInnsending(ytelseType.getKode(), saksnummer.getVerdi());
    }

    private List<JournalpostInnsendingEntitet> markerOgHentJournalposterKlarForInnsending(String ytelseType, String saksnummer) {
        Stream<JournalpostInnsendingEntitet> stream = entityManager
            .createQuery("select j from JournalpostInnsendingEntitet j where j.saksnummer=:saksnummer and j.ytelseType=:ytelseType and j.status=:status",
                JournalpostInnsendingEntitet.class)
            .setParameter("saksnummer", saksnummer)
            .setParameter("ytelseType", ytelseType)
            .setParameter("status", JournalpostInnsendingEntitet.Status.UBEHANDLET.getDbKode())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, "100") // magic - siden JPA ikke har native NOWAIT støtter hibernate det gjennom PESSIMISTIC_WRITE+
                                                              // timeout=0;
            .getResultStream();

        var entiteter = stream.collect(Collectors.toList());

        entiteter.forEach(e -> {
            e.setStatus(JournalpostInnsendingEntitet.Status.INNSENDT);
            entityManager.persist(e);
        });

        entityManager.flush();
        return entiteter;
    }

    public void lagreInnsending(JournalpostInnsendingEntitet innsending) {
        entityManager.persist(innsending);
        entityManager.flush();
    }

    public List<JournalpostMottattEntitet> markerJournalposterBehandlet(JournalpostId journalpostId) {
        Stream<JournalpostMottattEntitet> stream = entityManager
            .createQuery("select j from JournalpostMottattEntitet j where j.journalpostId=:journalpostId and j.status=:status", JournalpostMottattEntitet.class)
            .setParameter("journalpostId", journalpostId.getVerdi())
            .setParameter("status", JournalpostMottattEntitet.Status.UBEHANDLET.getDbKode())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, "100") // magic - siden JPA ikke har native NOWAIT støtter hibernate det gjennom PESSIMISTIC_WRITE+
                                                              // timeout=0;
            .getResultStream();

        var entiteter = stream.collect(Collectors.toList());

        entiteter.forEach(e -> {
            e.setStatus(JournalpostMottattEntitet.Status.BEHANDLET);
            entityManager.persist(e);
        });

        entityManager.flush();
        return entiteter;
    }

    public void lagreMottatt(JournalpostMottattEntitet mottatt) {
        entityManager.persist(mottatt);
        entityManager.flush();
    }

    public Optional<JournalpostMottattEntitet> finnJournalpostMottatt(JournalpostId journalPostId) {
        var list = entityManager
            .createQuery("select j from JournalpostMottattEntitet j where j.journalpostId=:journalpostId", JournalpostMottattEntitet.class)
            .setParameter("journalpostId", journalPostId.getVerdi())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, "100") // magic - siden JPA ikke har native NOWAIT støtter hibernate det gjennom PESSIMISTIC_WRITE+
                                                              // timeout=0;
            .getResultList();

        if (list.isEmpty()) {
            return Optional.empty();
        } else if (list.size() == 1) {
            return Optional.of(list.get(0));
        } else {
            throw new IllegalStateException("Fant mer enn ett innslag [" + list.size() + "] for journalpost: " + journalPostId);
        }
    }

    public List<JournalpostMottattEntitet> finnJournalposterMedAktørIdMottattEtterTidspunkt(String aktørId, LocalDateTime tidspunkt) {
        return entityManager
                .createQuery("select j from JournalpostMottattEntitet j where j.aktørId=:aktørId and j.mottattTidspunkt > :tidspunkt", JournalpostMottattEntitet.class)
                .setParameter("aktørId", aktørId)
                .setParameter("tidspunkt", tidspunkt)
                .getResultList();
    }

}
