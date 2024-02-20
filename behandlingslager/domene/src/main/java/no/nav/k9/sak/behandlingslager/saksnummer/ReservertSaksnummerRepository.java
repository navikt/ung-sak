package no.nav.k9.sak.behandlingslager.saksnummer;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class ReservertSaksnummerRepository {

    private EntityManager entityManager;

    ReservertSaksnummerRepository() {
        // for CDI proxy
    }

    @Inject
    public ReservertSaksnummerRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<ReservertSaksnummerEntitet> hent(Saksnummer saksnummer) {
        final TypedQuery<ReservertSaksnummerEntitet> query = entityManager.createQuery("FROM ReservertSaksnummer where saksnummer=:saksnummer", ReservertSaksnummerEntitet.class);
        query.setParameter("saksnummer", saksnummer);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagre(Saksnummer saksnummer, FagsakYtelseType ytelseType, String brukerAktørid, String pleietrengendeAktørId) {
        if (hent(saksnummer).isPresent()) {
            throw new IllegalArgumentException("Saksnummer er allerede reservert");
        }
        final var entitet = new ReservertSaksnummerEntitet(saksnummer.getVerdi(), ytelseType, brukerAktørid, pleietrengendeAktørId);
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
