package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.typer.AktørId;

@Dependent
public class SykdomVurderingRepository {

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
    
    public void lagre(SykdomVurderingVersjon versjon) {
        entityManager.persist(versjon);
        entityManager.flush();
    }

    public SykdomVurdering hentVurdering(Long vurderingId) {
        SykdomVurdering v = entityManager.find(SykdomVurdering.class, vurderingId);

        if (v == null) {
            throw new EntityNotFoundException("Finner ikke vurdering for ID " + vurderingId);
        }

        return v;
    }

    public Collection<SykdomVurderingVersjon> hentVurderingerFor(SykdomVurderingType sykdomVurderingType, UUID behandlingUuid, AktørId pleietrengende) {
        if (behandlingUuid != null) {
            return hentBehandlingVurderingerFor(sykdomVurderingType, behandlingUuid);
        } else {
            return hentSisteVurderingerFor(sykdomVurderingType, pleietrengende);
        }        
        
    }

    private Collection<SykdomVurderingVersjon> hentBehandlingVurderingerFor(SykdomVurderingType sykdomVurderingType, UUID behandlingUuid) {
        final TypedQuery<SykdomVurderingVersjon> q = entityManager.createQuery("SELECT vv From SykdomGrunnlagBehandling as sgb inner join sgb.grunnlag as sg inner join sg.vurderinger as vv inner join vv.sykdomVurdering as v where sgb.behandlingUuid = :behandlingUuid and v.type = :sykdomVurderingType and sgb.versjon = ( select max(sgb2.versjon) From SykdomGrunnlagBehandling as sgb2 where sgb2.behandlingUuid = sgb.behandlingUuid )", SykdomVurderingVersjon.class);
        q.setParameter("sykdomVurderingType", sykdomVurderingType);
        q.setParameter("behandlingUuid", behandlingUuid);
        return (Collection<SykdomVurderingVersjon>) q.getResultList();
    }

    private Collection<SykdomVurderingVersjon> hentSisteVurderingerFor(SykdomVurderingType sykdomVurderingType,
            AktørId pleietrengende) {
        final TypedQuery<SykdomVurderingVersjon> q = entityManager.createQuery("SELECT vv From SykdomVurderingVersjon as vv inner join vv.sykdomVurdering as v inner join v.sykdomVurderinger as sv inner join sv.person as p where p.aktoerId = :aktoerId and v.type = :sykdomVurderingType and vv.versjon = ( select max(vv2.versjon) From SykdomVurderingVersjon vv2 where vv2.sykdomVurdering = vv.sykdomVurdering )", SykdomVurderingVersjon.class);
        q.setParameter("sykdomVurderingType", sykdomVurderingType);
        q.setParameter("aktoerId", pleietrengende);
        return (Collection<SykdomVurderingVersjon>) q.getResultList();
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
