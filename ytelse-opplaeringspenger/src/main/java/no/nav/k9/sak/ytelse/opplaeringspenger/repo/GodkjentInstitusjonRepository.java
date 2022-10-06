package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class GodkjentInstitusjonRepository {

    private EntityManager entityManager;

    public GodkjentInstitusjonRepository() {
    }

    @Inject
    public GodkjentInstitusjonRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<GodkjentInstitusjon> hentMedNavn(String navn) {
        TypedQuery<GodkjentInstitusjon> query = entityManager.createQuery(
                "SELECT voi FROM GodkjentInstitusjon voi WHERE voi.navn = :navn",
                GodkjentInstitusjon.class)
            .setParameter("navn", navn);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public List<GodkjentInstitusjon> hentAlle() {
        TypedQuery<GodkjentInstitusjon> query = entityManager.createQuery(
                "SELECT gi FROM GodkjentInstitusjon gi",
                GodkjentInstitusjon.class);
        return query.getResultList();
    }
}
