package no.nav.k9.sak.mottak.repo;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class MottatteDokumentRepository {

    private EntityManager entityManager;

    @Inject
    public MottatteDokumentRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public MottattDokument lagre(MottattDokument mottattDokument, DokumentStatus status) {
        if (!mottattDokument.getStatus().erGyldigTransisjon(status)) {
            throw new IllegalArgumentException("Ugyldig transisjon: " + mottattDokument.getStatus() + " -> " + status);
        }
        mottattDokument.setStatus(Objects.requireNonNull(status, "status"));
        entityManager.persist(mottattDokument);
        entityManager.flush();
        return mottattDokument;
    }

    /** Lagrer kun, endrer ikke status . */
    public MottattDokument oppdater(MottattDokument mottattDokument) {
        if (mottattDokument.getId() == null) {
            throw new IllegalStateException("Kan kun oppdatere eksisterende dokument her, ikke lagre nytt: journalpostId=" + mottattDokument.getJournalpostId());
        }
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
    public List<MottattDokument> hentMottatteDokumentForBehandling(long fagsakId, long behandlingId, Brevkode type, boolean taSkriveLås, DokumentStatus... statuser) {
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :fagsakId AND m.behandlingId = :behandlingId AND m.type = :type AND(m.status IS NULL OR m.status IN (:status)) order by m.id";
        Set<DokumentStatus> statusParam = statuser == null || statuser.length == 0 ? EnumSet.complementOf(EnumSet.of(DokumentStatus.UGYLDIG)) : Set.of(statuser);
        var query = entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter("fagsakId", fagsakId)
            .setParameter("behandlingId", behandlingId)
            .setParameter("type", type)
            .setParameter("status", statusParam);
        if (taSkriveLås) {
            query.setLockMode(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        return query.getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedFagsakId(long fagsakId, DokumentStatus... statuser) {
        return hentMottatteDokumentMedFagsakId(fagsakId, false, statuser);
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedFagsakId(long fagsakId) {
        return hentMottatteDokumentMedFagsakId(fagsakId, DokumentStatus.GYLDIG);
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedFagsakId(long fagsakId, boolean taSkriveLås, DokumentStatus... statuser) {
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :param AND (m.status IS NULL OR m.status IN (:status)) order by m.id";
        Set<DokumentStatus> statusParam = statuser == null || statuser.length == 0 ? EnumSet.complementOf(EnumSet.of(DokumentStatus.UGYLDIG)) : Set.of(statuser);
        var query = entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter("param", fagsakId)
            .setParameter("status", statusParam);
        if (taSkriveLås) {
            query.setLockMode(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        return query.getResultList();
    }

    public List<MottattDokument> hentMottatteDokument(Long fagsakId, Collection<JournalpostId> journalpostIder) {
        return hentMottatteDokument(fagsakId, journalpostIder, DokumentStatus.GYLDIG);
    }

    public List<MottattDokument> hentMottatteDokument(Long fagsakId, Collection<JournalpostId> journalpostIder, DokumentStatus... statuser) {
        if (journalpostIder == null || journalpostIder.isEmpty()) {
            return Collections.emptyList();
        }
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :param AND (m.status IS NULL OR m.status IN (:status)) AND m.journalpostId IN (:journalpostIder)";
        Set<DokumentStatus> statusParam = statuser == null || statuser.length == 0 ? EnumSet.complementOf(EnumSet.of(DokumentStatus.UGYLDIG)) : Set.of(statuser);
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter("param", fagsakId)
            .setParameter("journalpostIder", journalpostIder)
            .setParameter("status", statusParam)
            .getResultList();
    }

    public void oppdaterStatus(List<MottattDokument> mottatteDokumenter, DokumentStatus status) {
        mottatteDokumenter.stream().forEach(m -> {
            m.setStatus(status);
            entityManager.persist(m);
        });
        entityManager.flush();

    }

}
