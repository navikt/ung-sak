package no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class OpplæringDokumentRepository {

    private final EntityManager entityManager;

    @Inject
    public OpplæringDokumentRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    public List<OpplæringDokument> hentDokumenterForSak(Saksnummer saksnummer) {
        final TypedQuery<OpplæringDokument> q = entityManager.createQuery(
            "SELECT d From OpplæringDokument d where d.søkersSaksnummer = :saksnummer", OpplæringDokument.class);

        q.setParameter("saksnummer", saksnummer);

        return q.getResultList();
    }

    public Optional<OpplæringDokument> hentDokument(Long dokumentId, Saksnummer saksnummer) {
        final TypedQuery<OpplæringDokument> q = entityManager.createQuery(
            "SELECT d From OpplæringDokument d where d.id = :dokumentId and d.søkersSaksnummer = :saksnummer", OpplæringDokument.class);

        q.setParameter("dokumentId", dokumentId);
        q.setParameter("saksnummer", saksnummer);

        return q.getResultList().stream().findFirst();
    }

    public void lagre(OpplæringDokument dokument) {
        if (dokument.getId() != null) {
            throw new IllegalStateException("Dokumentet har allerede blitt lagret.");
        }
        entityManager.persist(dokument);
        entityManager.flush();
    }

    public boolean finnesDokument(JournalpostId journalpostId, String dokumentInfoId) {
        Objects.requireNonNull(journalpostId, "journalpostId");

        final String dokumentInfoSjekk = (dokumentInfoId == null) ? "d.dokumentInfoId IS NULL" : "d.dokumentInfoId = :dokumentInfoId";

        final TypedQuery<OpplæringDokument> q = entityManager.createQuery(
            "SELECT d From OpplæringDokument as d "
                + "where d.journalpostId = :journalpostId"
                + "  and " + dokumentInfoSjekk, OpplæringDokument.class);

        q.setParameter("journalpostId", journalpostId);
        if (dokumentInfoId != null) {
            q.setParameter("dokumentInfoId", dokumentInfoId);
        }

        Optional<OpplæringDokument> dokument = q.getResultList().stream().findFirst();

        return dokument.isPresent();
    }
}
