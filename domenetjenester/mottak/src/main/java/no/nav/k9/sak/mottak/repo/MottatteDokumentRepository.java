package no.nav.k9.sak.mottak.repo;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class MottatteDokumentRepository {

    private EntityManager entityManager;

    public MottatteDokumentRepository() {
        // for CDI proxy
    }

    @Inject
    public MottatteDokumentRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public MottattDokument lagre(MottattDokument mottattDokument) {
        entityManager.persist(mottattDokument);
        entityManager.flush();

        return mottattDokument;
    }

    public Optional<MottattDokument> hentMottattDokument(long mottattDokumentId) {
        TypedQuery<MottattDokument> query = entityManager.createQuery(
            "select m from MottattDokument m where m.id = :param", MottattDokument.class)
            .setParameter("param", mottattDokumentId) // NOSONAR
        ;
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedFagsakId(long fagsakId) {
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :param AND (m.status IS NULL OR m.status=:gyldig)";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter("param", fagsakId) // NOSONAR
            .setParameter("gyldig", DokumentStatus.GYLDIG) // NOSONAR
            .getResultList();
    }

    public List<MottattDokument> hentMottatteDokument(Long fagsakId, Collection<JournalpostId> journalpostIder) {
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :param AND (m.status IS NULL OR m.status=:gyldig) AND m.journalpostId IN (:journalpostIder)";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter("param", fagsakId) // NOSONAR
            .setParameter("journalpostIder", journalpostIder)
            .setParameter("gyldig", DokumentStatus.GYLDIG) // NOSONAR
            .getResultList();
    }

}
