package no.nav.k9.sak.behandlingslager.saksnummer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;
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

    public List<ReservertSaksnummerEntitet> hent(AktørId brukerAktørId) {
        final TypedQuery<ReservertSaksnummerEntitet> query = entityManager.createQuery("FROM ReservertSaksnummer where brukerAktørId=:brukerAktørId and slettet=false", ReservertSaksnummerEntitet.class);
        query.setParameter("brukerAktørId", brukerAktørId);
        return query.getResultList();
    }

    public Optional<ReservertSaksnummerEntitet> hent(FagsakYtelseType ytelseType, String brukerAktørId, String pleietrengendeAktørId, String relatertPersonAktørId, String behandlingsår) {
        return switch (ytelseType) {
            case PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OPPLÆRINGSPENGER -> hentForPleiepenger(ytelseType, brukerAktørId, pleietrengendeAktørId);
            case OMSORGSPENGER -> hentForOMP(ytelseType, brukerAktørId, behandlingsår);
            case OMSORGSPENGER_MA -> hentForOMPMA(ytelseType, brukerAktørId, relatertPersonAktørId, behandlingsår);
            case OMSORGSPENGER_KS, OMSORGSPENGER_AO -> hentForOMPKSAO(ytelseType, brukerAktørId, pleietrengendeAktørId, behandlingsår);
            default -> throw new IllegalArgumentException("Ikke støttet ytelsetype: " + ytelseType);
        };
    }

    private Optional<ReservertSaksnummerEntitet> hentForPleiepenger(FagsakYtelseType ytelseType, String brukerAktørId, String pleietrengendeAktørId) {
        if (pleietrengendeAktørId == null) {
            return Optional.empty();
        }
        final TypedQuery<ReservertSaksnummerEntitet> query = entityManager.createQuery("FROM ReservertSaksnummer where ytelseType=:ytelseType and brukerAktørId=:brukerAktørId and pleietrengendeAktørId=:pleietrengendeAktørId and slettet=false", ReservertSaksnummerEntitet.class);
        query.setParameter("ytelseType", ytelseType);
        query.setParameter("brukerAktørId", new AktørId(brukerAktørId));
        query.setParameter("pleietrengendeAktørId", new AktørId(pleietrengendeAktørId));
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<ReservertSaksnummerEntitet> hentForOMP(FagsakYtelseType ytelseType, String brukerAktørId, String behandlingsår) {
        if (behandlingsår == null) {
            return Optional.empty();
        }
        final TypedQuery<ReservertSaksnummerEntitet> query = entityManager.createQuery("FROM ReservertSaksnummer where ytelseType=:ytelseType and brukerAktørId=:brukerAktørId and behandlingsår=:behandlingsår and slettet=false", ReservertSaksnummerEntitet.class);
        query.setParameter("ytelseType", ytelseType);
        query.setParameter("brukerAktørId", new AktørId(brukerAktørId));
        query.setParameter("behandlingsår", behandlingsår);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<ReservertSaksnummerEntitet> hentForOMPMA(FagsakYtelseType ytelseType, String brukerAktørId, String relatertPersonAktørId, String behandlingsår) {
        if (behandlingsår == null || relatertPersonAktørId == null) {
            return Optional.empty();
        }
        final TypedQuery<ReservertSaksnummerEntitet> query = entityManager.createQuery("FROM ReservertSaksnummer where ytelseType=:ytelseType and brukerAktørId=:brukerAktørId and relatertPersonAktørId=:relatertPersonAktørId and behandlingsår=:behandlingsår and slettet=false", ReservertSaksnummerEntitet.class);
        query.setParameter("ytelseType", ytelseType);
        query.setParameter("brukerAktørId", new AktørId(brukerAktørId));
        query.setParameter("relatertPersonAktørId", new AktørId(relatertPersonAktørId));
        query.setParameter("behandlingsår", behandlingsår);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<ReservertSaksnummerEntitet> hentForOMPKSAO(FagsakYtelseType ytelseType, String brukerAktørId, String pleietrengendeAktørId, String behandlingsår) {
        if (pleietrengendeAktørId == null || behandlingsår == null) {
            return Optional.empty();
        }
        final TypedQuery<ReservertSaksnummerEntitet> query = entityManager.createQuery("FROM ReservertSaksnummer where ytelseType=:ytelseType and brukerAktørId=:brukerAktørId and pleietrengendeAktørId=:pleietrengendeAktørId and behandlingsår=:behandlingsår and slettet=false", ReservertSaksnummerEntitet.class);
        query.setParameter("ytelseType", ytelseType);
        query.setParameter("brukerAktørId", new AktørId(brukerAktørId));
        query.setParameter("pleietrengendeAktørId", new AktørId(pleietrengendeAktørId));
        query.setParameter("behandlingsår", behandlingsår);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagre(Saksnummer saksnummer, FagsakYtelseType ytelseType, String brukerAktørId, String pleietrengendeAktørId, String relatertPersonAktørId, String behandlingsår) {
        if (hent(saksnummer).isPresent()) {
            throw new IllegalArgumentException("Saksnummer er allerede reservert");
        }
        final var entitet = new ReservertSaksnummerEntitet(saksnummer, ytelseType, brukerAktørId, pleietrengendeAktørId, relatertPersonAktørId, behandlingsår);
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
