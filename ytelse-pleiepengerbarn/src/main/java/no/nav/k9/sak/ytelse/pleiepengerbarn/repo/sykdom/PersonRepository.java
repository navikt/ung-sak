package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class PersonRepository {

    private EntityManager entityManager;

    PersonRepository() {
        //CDI
    }

    @Inject
    public PersonRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManagern");
        this.entityManager = entityManager;
    }

    public Person hentEllerLagrePerson(AktørId aktørId) {
        return hentEllerLagre(new Person(aktørId, null));
    }

    public Person hentEllerLagre(Person person) {
        final EntityManager innerEntityManager = entityManager.getEntityManagerFactory().createEntityManager();
        final EntityTransaction transaction = innerEntityManager.getTransaction();
        transaction.begin();
        try {
            final Query q = innerEntityManager.createNativeQuery("INSERT INTO PERSON (ID, AKTOER_ID, NORSK_IDENTITETSNUMMER) VALUES (nextval('SEQ_PERSON'), :aktorId, :norskIdentitetsnummer) ON CONFLICT DO NOTHING");
            q.setParameter("aktorId", person.getAktørId().getId());
            q.setParameter("norskIdentitetsnummer", person.getNorskIdentitetsnummer());
            q.executeUpdate();
            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
            throw t;
        } finally {
            innerEntityManager.close();
        }

        return findPerson(person.getAktørId());
    }

    private Person findPerson(AktørId aktørId) {
        final TypedQuery<Person> q = entityManager.createQuery("select p From Person p where p.aktørId = :aktørId", Person.class);
        q.setParameter("aktørId", aktørId);
        return q.getResultList().stream().findFirst().orElse(null);
    }
}
