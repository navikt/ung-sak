package no.nav.k9.sak.behandlingslager.behandling.historikk;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class HistorikkRepository {
    private EntityManager entityManager;

    HistorikkRepository() {
        // for CDI proxy
    }

    @Inject
    public HistorikkRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public void lagre(Historikkinnslag historikkinnslag) {
        if (historikkinnslag.getFagsakId() == null) {
            historikkinnslag.setFagsakId(getFagsakId(historikkinnslag.getBehandlingId()));
        }

        entityManager.persist(historikkinnslag);
        for (HistorikkinnslagDel historikkinnslagDel : historikkinnslag.getHistorikkinnslagDeler()) {
            entityManager.persist(historikkinnslagDel);
            for (HistorikkinnslagFelt historikkinnslagFelt : historikkinnslagDel.getHistorikkinnslagFelt()) {
                entityManager.persist(historikkinnslagFelt);
            }
        }
        entityManager.flush();
    }

    public boolean finnesUuidAllerede(UUID historikkUuid) {
        TypedQuery<Historikkinnslag> query = entityManager.createQuery("from Historikkinnslag where uuid=:historikkUuid", Historikkinnslag.class);
        query.setParameter("historikkUuid", historikkUuid);
        return !query.getResultList().isEmpty();
    }

    public List<Historikkinnslag> hentHistorikk(Long behandlingId) {

        Long fagsakId = getFagsakId(behandlingId);

        return entityManager.createQuery(
            "select h from Historikkinnslag h where (h.behandlingId = :behandlingId OR h.behandlingId = NULL) AND h.fagsakId = :fagsakId ", //$NON-NLS-1$
            Historikkinnslag.class)
            .setParameter("fagsakId", fagsakId)// NOSONAR //$NON-NLS-1$
            .setParameter("behandlingId", behandlingId) //$NON-NLS-1$
            .getResultList();
    }

    private Long getFagsakId(long behandlingId) {
        return entityManager.createQuery("select b.fagsak.id from Behandling b where b.id = :behandlingId", Long.class) //$NON-NLS-1$
                .setParameter("behandlingId", behandlingId) // NOSONAR
                .getSingleResult();
    }

    public List<Historikkinnslag> hentHistorikkForSaksnummer(Saksnummer saksnummer) {
        return entityManager.createQuery(
                "select h from Historikkinnslag h inner join Fagsak f On f.id = h.fagsakId where f.saksnummer= :saksnummer",
                Historikkinnslag.class)
                .setParameter("saksnummer", saksnummer)
                .getResultList();
    }
}
