package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class BostedsGrunnlagRepository {

    private final EntityManager entityManager;

    @Inject
    public BostedsGrunnlagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<BostedsGrunnlag> hentGrunnlagHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM BostedsGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId AND g.aktiv = true",
            BostedsGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagrer bostedsavklaringer for en behandling.
     * Avklaringene er periodisert – én per skjæringstidspunkt.
     * Dersom grunnlaget endres (avklaringer er ulike), opprettes nye entiteter.
     * Dersom avklaringene er uendret, beholdes eksisterende grunnlag.
     *
     * @param behandlingId         Behandling ID
     * @param avklaringerPerSkjæringstidspunkt Map fra skjæringstidspunkt til erBosattITrondheim
     * @return Grunnlagsreferansen (ny ved endring, eksisterende ved uendret)
     */
    public UUID lagreAvklaringer(Long behandlingId, Map<LocalDate, Boolean> avklaringerPerSkjæringstidspunkt) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        var nyHolder = new BostedsAvklaringHolder();
        for (var entry : avklaringerPerSkjæringstidspunkt.entrySet()) {
            nyHolder.leggTilAvklaring(new BostedsAvklaring(entry.getKey(), entry.getValue()));
        }

        if (eksisterende.isPresent() && eksisterende.get().getHolder().equals(nyHolder)) {
            return eksisterende.get().getGrunnlagsreferanse();
        }

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, nyHolder, UUID.randomUUID());
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
        return nyttGrunnlag.getGrunnlagsreferanse();
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Peker til samme holder (aggregat) – ingen kopiering av data.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new BostedsGrunnlag(nyBehandlingId, eksisterende.getHolder(), eksisterende.getGrunnlagsreferanse());
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }

    private void deaktiverEksisterende(BostedsGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
