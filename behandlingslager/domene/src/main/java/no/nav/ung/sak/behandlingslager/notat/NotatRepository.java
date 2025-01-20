package no.nav.ung.sak.behandlingslager.notat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

@Dependent
public class NotatRepository {

    private final EntityManager entityManager;

    @Inject
    public NotatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public UUID lagre(NotatEntitet notat) {
        entityManager.persist(notat);
        entityManager.flush();
        return notat.getUuid();
    }

    public List<NotatEntitet> hentForSakOgAktør(Fagsak fagsak) {
        TypedQuery<NotatSakEntitet> sakNotat = entityManager.createQuery(
            "from NotatSakEntitet n where n.fagsakId = :fagsakId and aktiv = true", NotatSakEntitet.class);
        sakNotat.setParameter("fagsakId", fagsak.getId());
        List<NotatEntitet> resultat = new ArrayList<>(sakNotat.getResultList());
        resultat.sort(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt).reversed());
        return resultat;
    }

}
