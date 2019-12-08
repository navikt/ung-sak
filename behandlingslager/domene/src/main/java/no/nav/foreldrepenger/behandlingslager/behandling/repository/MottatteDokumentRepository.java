package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class MottatteDokumentRepository {

    private EntityManager entityManager;

    private static final String PARAM_KEY = "param";

    public MottatteDokumentRepository() {
        // for CDI proxy
    }

    @Inject
    public MottatteDokumentRepository(@VLPersistenceUnit EntityManager entityManager) {
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
            .setParameter(PARAM_KEY, mottattDokumentId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokument(long behandlingId) {
        String strQueryTemplate = "select m from MottattDokument m where m.behandlingId = :param";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter(PARAM_KEY, behandlingId)
            .getResultList();
    }
    
    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottattDokument(JournalpostId journalpostId) {
        var query = entityManager.createQuery( "select m from MottattDokument m where m.journalpostId = :param", MottattDokument.class)
            .setParameter(PARAM_KEY, journalpostId);
        return query.getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedFagsakId(long fagsakId) {
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :param";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter(PARAM_KEY, fagsakId)
            .getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedForsendelseId(UUID forsendelseId) {
        String strQueryTemplate = "select m from MottattDokument m where m.forsendelseId = :param";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter(PARAM_KEY, forsendelseId)
            .getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    @SuppressWarnings("unchecked")
    public List<MottattDokument> hentMottatteDokumentVedleggPåBehandlingId(long behandlingId) {
        Query query = entityManager.createNativeQuery(
            "SELECT md.* FROM MOTTATT_DOKUMENT md WHERE md.behandling_id = :param AND md.type IN (:dokumentTyper)", //$NON-NLS-1$
            MottattDokument.class)
            .setParameter("dokumentTyper", DokumentTypeId.getVedleggTyper())
            .setParameter(PARAM_KEY, behandlingId); //$NON-NLS-1$

        return query.getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * 
     * Henter alle dokument med type som ikke er søknad, endringssøknad, klage, IM (eller udefinert)
     * 
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    @SuppressWarnings("unchecked")
    public List<MottattDokument> hentMottatteDokumentAndreTyperPåBehandlingId(long behandlingId) {
        Query query = entityManager.createNativeQuery(
            "SELECT md.* FROM MottattDokument md WHERE md.behandling_id = :param " +
                "AND md.type NOT IN (:dokumentTyper) " +
                "AND md.type != :udefinert", //$NON-NLS-1$
            MottattDokument.class)
            .setParameter("dokumentTyper", DokumentTypeId.getSpesialTyperKoder())
            .setParameter("udefinert", DokumentTypeId.UDEFINERT.getKode())
            .setParameter(PARAM_KEY, behandlingId); //$NON-NLS-1$

        return query.getResultList();
    }

    public void oppdaterMedBehandling(MottattDokument mottattDokument, long behandlingId) {
        entityManager.createQuery(
            "update MottattDokument set behandlingId = :param WHERE id = :dokumentId")
            .setParameter("dokumentId", mottattDokument.getId())
            .setParameter(PARAM_KEY, behandlingId)
            .executeUpdate();
    }

    public void oppdaterMedKanalreferanse(MottattDokument mottattDokument, String kanalreferanse) {
        entityManager.createQuery(
            "update MottattDokument set kanalreferanse = :param WHERE id = :dokumentId")
            .setParameter("dokumentId", mottattDokument.getId())
            .setParameter(PARAM_KEY, kanalreferanse)
            .executeUpdate();
    }
}
