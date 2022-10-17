package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class GodkjentOpplæringsinstitusjonRepository {

    private EntityManager entityManager;

    public GodkjentOpplæringsinstitusjonRepository() {
    }

    @Inject
    public GodkjentOpplæringsinstitusjonRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<GodkjentOpplæringsinstitusjon> hentMedUuid(UUID uuid) {
        TypedQuery<GodkjentOpplæringsinstitusjon> query = entityManager.createQuery(
                "SELECT goi FROM GodkjentOpplæringsinstitusjon goi WHERE goi.uuid = :uuid",
                GodkjentOpplæringsinstitusjon.class)
            .setParameter("uuid", uuid);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public List<GodkjentOpplæringsinstitusjon> hentAlle() {
        TypedQuery<GodkjentOpplæringsinstitusjon> query = entityManager.createQuery(
                "SELECT goi FROM GodkjentOpplæringsinstitusjon goi",
                GodkjentOpplæringsinstitusjon.class);
        return query.getResultList();
    }
}
