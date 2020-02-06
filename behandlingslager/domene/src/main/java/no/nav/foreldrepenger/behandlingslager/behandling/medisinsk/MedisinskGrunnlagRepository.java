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

    public void lagreOgFlush(Behandling behandling, KontinuerligTilsyn kontinuerligTilsyn, Legeerklæringer legeerklæringer) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$
        final Optional<MedisinskGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandling.getId());
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final MedisinskGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final MedisinskGrunnlag grunnlagEntitet = new MedisinskGrunnlag(behandling, kontinuerligTilsyn, legeerklæringer);
        entityManager.persist(kontinuerligTilsyn);
        entityManager.persist(legeerklæringer);
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
        søknadEntitet.ifPresent(entitet -> lagreOgFlush(nyBehandling, entitet.getKontinuerligTilsyn(), entitet.getLegeerklæringer()));
    }
}
