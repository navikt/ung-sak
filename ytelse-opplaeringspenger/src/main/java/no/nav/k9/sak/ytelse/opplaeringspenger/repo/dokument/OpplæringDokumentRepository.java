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

    public Optional<OpplæringDokument> hentDokument(Long dokumentId) {
        final TypedQuery<OpplæringDokument> q = entityManager.createQuery(
            "SELECT d From OpplæringDokument d where d.id = :dokumentId", OpplæringDokument.class);

        q.setParameter("dokumentId", dokumentId);

        return q.getResultList().stream().findFirst();
    }

    public void lagre(OpplæringDokument dokument) {
        if (dokument.getId() != null) {
            throw new IllegalStateException("Dokumentet har allerede blitt lagret.");
        }
        entityManager.persist(dokument);
        entityManager.flush();
    }

    public void oppdater(OpplæringDokumentInformasjon dokumentInformasjon) {
        if (dokumentInformasjon.getDokument().getId() == null) {
            throw new IllegalStateException("Kan ikke oppdatere dokument som ikke har vært lagret før.");
        }
        entityManager.persist(dokumentInformasjon);
        entityManager.flush();
    }

    public List<OpplæringDokument> hentDuplikaterAv(Long dokumentId) {
        final TypedQuery<OpplæringDokument> q = entityManager.createQuery(
            "SELECT d From OpplæringDokument as d "
                + "inner join d.informasjon as i "
                + "inner join i.duplikatAvDokument as dd "
                + "where dd.id = :dokumentId"
                + "  and i.versjon = ("
                + "    select max(i2.versjon) "
                + "    From OpplæringDokumentInformasjon as i2 "
                + "    where i2.dokument = i.dokument"
                + "  )", OpplæringDokument.class);

        q.setParameter("dokumentId", dokumentId);
        return q.getResultList();
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
