package no.nav.ung.sak.behandlingslager.fritekst;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;

import java.util.Objects;
import java.util.Optional;

@Dependent
public class FritekstRepository {

    private EntityManager entityManager;

    protected FritekstRepository() {
        // for CDI proxy
    }

    @Inject
    public FritekstRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public void lagre(long behandlingId, KlageVurdertAv vurdertAv, String fritekst) {
        String skrevetAv = vurdertAv.getKode();

        hent(behandlingId, skrevetAv).ifPresentOrElse(fritekstEntitet ->
            fritekstEntitet.setFritekst(fritekst),
            () -> entityManager.persist(new FritekstEntitet(behandlingId, skrevetAv, fritekst)));
        entityManager.flush();
    }

    private Optional<FritekstEntitet> hent(Long behandlingId, String skrevetAv) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(skrevetAv, "skrevetAv");

        final TypedQuery<FritekstEntitet> query = entityManager.createQuery(
            " FROM FritekstEntitet" +
                "        WHERE behandlingId = :behandlingId " +
                "        AND skrevetAv = :skrevetAv",
            FritekstEntitet.class);// NOSONAR
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("skrevetAv", skrevetAv);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<String> hentFritekst(Long behandlingId, String skrevetAv) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(skrevetAv, "skrevetAv");

        final TypedQuery<FritekstEntitet> query = entityManager.createQuery(
            " FROM FritekstEntitet" +
                "        WHERE behandlingId = :behandlingId " +
                "        AND skrevetAv = :skrevetAv",
            FritekstEntitet.class);// NOSONAR
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("skrevetAv", skrevetAv);
        return HibernateVerktøy.hentUniktResultat(query).map(FritekstEntitet::getFritekst);
    }

    private void slett(Long behandlingId, String skrevetAv) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(skrevetAv, "skrevetAv");

        final TypedQuery<FritekstEntitet> query = entityManager.createQuery(
            " DELETE FROM FritekstEntitet" +
                "        WHERE behandlingId = :behandlingId " +
                "        AND skrevetAv = :skrevetAv",
            FritekstEntitet.class);// NOSONAR
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("skrevetAv", skrevetAv);
        query.executeUpdate();
    }
}
