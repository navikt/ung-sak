package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class MedisinskGrunnlagRepository {

    private EntityManager entityManager;

    MedisinskGrunnlagRepository() {
        // CDI
    }

    @Inject
    public MedisinskGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public MedisinskGrunnlag hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<MedisinskGrunnlag> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Behandling behandling, Pleietrengende pleietrengende) {
        final var medisinskGrunnlag = hentEksisterendeGrunnlag(behandling.getId());

        final var legeerklæringer = medisinskGrunnlag.map(MedisinskGrunnlag::getLegeerklæringer).orElse(null);
        final var kontinuerligTilsyn = medisinskGrunnlag.map(MedisinskGrunnlag::getKontinuerligTilsyn).orElse(null);
        final var omsorgenFor = medisinskGrunnlag.map(MedisinskGrunnlag::getOmsorgenFor).orElse(null);
        lagre(behandling, kontinuerligTilsyn, legeerklæringer, pleietrengende, omsorgenFor);
    }

    public void lagre(Behandling behandling, OmsorgenFor omsorgenFor) {
        final var medisinskGrunnlag = hentEksisterendeGrunnlag(behandling.getId());

        final var legeerklæringer = medisinskGrunnlag.map(MedisinskGrunnlag::getLegeerklæringer).orElse(null);
        final var kontinuerligTilsyn = medisinskGrunnlag.map(MedisinskGrunnlag::getKontinuerligTilsyn).orElse(null);
        final var pleietrengende = medisinskGrunnlag.map(MedisinskGrunnlag::getPleietrengende).orElse(null);
        lagre(behandling, kontinuerligTilsyn, legeerklæringer, pleietrengende, omsorgenFor);
    }

    public void lagre(Behandling behandling, KontinuerligTilsynBuilder kontinuerligTilsyn, Legeerklæringer legeerklæringer) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(kontinuerligTilsyn, "kontinuerligTilsyn"); // NOSONAR $NON-NLS-1$

        final var medisinskGrunnlag = hentEksisterendeGrunnlag(behandling.getId());
        final var omsorgenFor = medisinskGrunnlag.map(MedisinskGrunnlag::getOmsorgenFor).orElse(null);
        final var pleietrengende = medisinskGrunnlag.map(MedisinskGrunnlag::getPleietrengende).orElse(null);

        lagre(behandling, kontinuerligTilsyn.build(), legeerklæringer, pleietrengende, omsorgenFor);
    }

    private void lagre(Behandling behandling, KontinuerligTilsyn kontinuerligTilsyn, Legeerklæringer legeerklæringer, Pleietrengende pleietrengende, OmsorgenFor omsorgenFor) {

        final Optional<MedisinskGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandling.getId());
        var pleie = pleietrengende;
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final MedisinskGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            pleie = eksisterendeGrunnlagEntitet.getPleietrengende();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final MedisinskGrunnlag grunnlagEntitet = new MedisinskGrunnlag(behandling, pleie, kontinuerligTilsyn, legeerklæringer, omsorgenFor);
        if (kontinuerligTilsyn != null) {
            entityManager.persist(kontinuerligTilsyn);
        }
        if (legeerklæringer != null) {
            entityManager.persist(legeerklæringer);
        }
        if (omsorgenFor != null) {
            entityManager.persist(omsorgenFor);
        }
        if (pleie != null) {
            entityManager.persist(pleie);
        }
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<MedisinskGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<MedisinskGrunnlag> query = entityManager.createQuery(
            "FROM MedisinskGrunnlag s " +
                "WHERE s.behandling.id = :behandlingId AND s.aktiv = true", MedisinskGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling gammelBehandling, Behandling nyBehandling) {
        Optional<MedisinskGrunnlag> søknadEntitet = hentEksisterendeGrunnlag(gammelBehandling.getId());
        søknadEntitet.ifPresent(entitet -> lagre(nyBehandling, new KontinuerligTilsyn(entitet.getKontinuerligTilsyn()),
            entitet.getLegeerklæringer(),
            entitet.getPleietrengende(), entitet.getOmsorgenFor()));
    }
}
