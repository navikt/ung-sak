package no.nav.k9.sak.behandlingslager.notat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

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

        if (fagsak.getPleietrengendeAktørId() != null) {
            TypedQuery<NotatAktørEntitet> aktoerNotat = entityManager.createQuery(
                "from NotatAktørEntitet n where n.aktørId = :aktørId and ytelseType = :ytelseType and aktiv = true", NotatAktørEntitet.class);
            aktoerNotat.setParameter("aktørId", fagsak.getPleietrengendeAktørId());
            aktoerNotat.setParameter("ytelseType", fagsak.getYtelseType());
            resultat.addAll(aktoerNotat.getResultList());
        }

        return resultat;
    }


    //TODO test at endring fører til flere versjoner

    //TODO test optimistisk låsing


}
