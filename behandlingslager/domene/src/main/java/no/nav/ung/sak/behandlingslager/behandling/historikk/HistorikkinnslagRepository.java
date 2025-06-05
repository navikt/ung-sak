package no.nav.ung.sak.behandlingslager.behandling.historikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.List;

@ApplicationScoped
public class HistorikkinnslagRepository {

    private EntityManager entityManager;

    @Inject
    public HistorikkinnslagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public HistorikkinnslagRepository() {
        //CDI
    }

    public List<Historikkinnslag> hent(Saksnummer saksnummer) {
        return entityManager.createQuery("select h from Historikkinnslag h inner join Fagsak f On f.id = h.fagsakId where f.saksnummer= :saksnummer",
            Historikkinnslag.class).setParameter("saksnummer", saksnummer).getResultStream().toList();
    }

    public List<Historikkinnslag> hent(Long behandlingId) {
        return entityManager.createQuery(
                "select h from Historikkinnslag h where h.behandlingId = :behandlingId",
                Historikkinnslag.class)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
    }

    public void lagre(Historikkinnslag historikkinnslag) {
        entityManager.persist(historikkinnslag);
        for (var linje : historikkinnslag.getLinjer()) {
            entityManager.persist(linje);
        }
        for (var dokument : historikkinnslag.getDokumentLinker()) {
            entityManager.persist(dokument);
        }
        entityManager.flush();
    }
}
