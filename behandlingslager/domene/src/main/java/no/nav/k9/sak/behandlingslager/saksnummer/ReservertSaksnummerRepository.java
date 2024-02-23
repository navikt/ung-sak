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

    private final EntityManager entityManager;

    @Inject
    public ReservertSaksnummerRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<ReservertSaksnummerEntitet> hent(Saksnummer saksnummer) {
        final TypedQuery<ReservertSaksnummerEntitet> query = entityManager.createQuery("FROM ReservertSaksnummer where saksnummer=:saksnummer and slettet=false", ReservertSaksnummerEntitet.class);
        query.setParameter("saksnummer", saksnummer);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagre(Saksnummer saksnummer, FagsakYtelseType ytelseType, String brukerAktørId, String pleietrengendeAktørId, String behandlingsår) {
        if (hent(saksnummer).isPresent()) {
            throw new IllegalArgumentException("Saksnummer er allerede reservert");
        }
        final var entitet = new ReservertSaksnummerEntitet(saksnummer, ytelseType, brukerAktørId, pleietrengendeAktørId, behandlingsår);
        entityManager.persist(entitet);
        entityManager.flush();
    }

    public void slettHvisEksisterer(Saksnummer saksnummer) {
        if (saksnummer == null) {
            return;
        }
        final var opt = hent(saksnummer);
        if (opt.isPresent()) {
            final var entitet = opt.get();
            entitet.setSlettet();
            entityManager.persist(entitet);
            entityManager.flush();
        }
    }
}
