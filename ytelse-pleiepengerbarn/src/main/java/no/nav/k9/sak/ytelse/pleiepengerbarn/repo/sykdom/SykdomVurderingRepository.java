package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;


import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
class SykdomVurderingRepository {

    private EntityManager entityManager;

    SykdomVurderingRepository() {
        // CDI
    }

    @Inject
    public SykdomVurderingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    /////////////////////////////

    public SykdomVurderinger hentVurderingerForBarn(Long personId) {
        return null;
    }

    public void lagre(SykdomVurdering vurdering) {
        entityManager.persist(vurdering);
        entityManager.flush();
    }

    public SykdomVurdering hentVurdering(Long vurderingId) {
        SykdomVurdering v = entityManager.find(SykdomVurdering.class, vurderingId);

        if (v == null) {
            throw new EntityNotFoundException("Finner ikke vurdering for ID " + vurderingId);
        }

        return v;
    }


    public SykdomVurderingVersjon hentVurderingVersjon(Long vurderingVersjonId) {
        return null;
    }

    public void lagre(SykdomVurderinger vurderinger) {
        entityManager.persist(vurderinger);
        entityManager.flush();
    }

    public SykdomPerson lagreEllerOppdater(SykdomPerson person) {

        EntityManager innerEntityManager = entityManager.getEntityManagerFactory().createEntityManager();
        innerEntityManager.getTransaction().begin();
        try {
            SykdomPerson eksisterendePerson = findPerson(innerEntityManager, person.getNorskIdentitetsnummer());
            if(eksisterendePerson == null) {
                innerEntityManager.persist(person);
            }
            innerEntityManager.getTransaction().commit();
        } catch (PersistenceException e) {
            innerEntityManager.getTransaction().rollback();
            SykdomPerson eksisterendePerson = findPerson(innerEntityManager, person.getNorskIdentitetsnummer());
            if(eksisterendePerson == null) {
                throw e;
            } else {
                return eksisterendePerson;
            }
        } catch (Throwable t) {
            innerEntityManager.getTransaction().rollback();
            throw t;
        } finally {
            innerEntityManager.close();
        }

        return findPerson(entityManager, person.getNorskIdentitetsnummer());
    }

    private SykdomPerson findPerson(EntityManager em, String identitetsnummer) {
        Query q = em.createQuery("SELECT p From SykdomPerson p where p.norskIdentitetsnummer = :idNummer");
        q.setParameter("idNummer", identitetsnummer);
        ArrayList<SykdomPerson> liste = (ArrayList<SykdomPerson>) q.getResultList();
        return liste.size() > 0 ? liste.get(0) : null;
    }

}
