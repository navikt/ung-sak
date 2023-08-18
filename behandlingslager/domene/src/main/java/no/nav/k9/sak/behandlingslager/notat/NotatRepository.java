package no.nav.k9.sak.behandlingslager.notat;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class NotatRepository {

    private final EntityManager entityManager;

    @Inject
    public NotatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long opprett(NotatEntitet notat) {
        entityManager.persist(notat);
        entityManager.flush();
        return notat.getId();
    }

    public NotatEntitet hent(long notatId) {
        TypedQuery<NotatEntitet> query = entityManager.createQuery(
            "select n from NotatEntitet n where n.id = :notatId and aktiv = true", NotatEntitet.class);
        query.setParameter("notatId", notatId);
        return HibernateVerktøy.hentEksaktResultat(query);

    }

    public void oppdater(NotatEntitet notat) {
        entityManager.merge(notat);
        entityManager.flush();
    }

    public List<NotatEntitet> hentForSakOgAktør(long fagsakId, AktørId pleietrengendeAktørId) {
        TypedQuery<NotatEntitet> fagsak = entityManager.createQuery(
            "select n from NotatEntitet n where (n.fagsakId = :fagsakId OR n.gjelder = :aktørId) and aktiv = true", NotatEntitet.class);
        fagsak.setParameter("fagsakId", fagsakId);
        fagsak.setParameter("aktørId", pleietrengendeAktørId);



        return fagsak.getResultList();
    }
}
