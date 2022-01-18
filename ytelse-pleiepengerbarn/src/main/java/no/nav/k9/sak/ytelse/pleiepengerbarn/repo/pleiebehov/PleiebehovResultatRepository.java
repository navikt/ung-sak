package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import no.nav.k9.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class PleiebehovResultatRepository {

    private EntityManager entityManager;

    PleiebehovResultatRepository() {
        // CDI
    }

    @Inject
    public PleiebehovResultatRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<PleiebehovResultat> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagreOgFlush(Long behandlingId, EtablertPleiebehovBuilder pleiebehovBuilder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(pleiebehovBuilder, "pleiebehovBuilder"); // NOSONAR $NON-NLS-1$
        final Optional<PleiebehovResultat> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final PleiebehovResultat eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final var tilsyn = pleiebehovBuilder.build();
        final PleiebehovResultat grunnlagEntitet = new PleiebehovResultat(behandlingId, tilsyn);
        entityManager.persist(tilsyn);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<PleiebehovResultat> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<PleiebehovResultat> query = entityManager.createQuery(
            "FROM PleiebehovResultat s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", PleiebehovResultat.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<PleiebehovResultat> søknadEntitet = hentEksisterendeGrunnlag(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> lagreOgFlush(nyBehandlingId, new EtablertPleiebehovBuilder(entitet.getPleieperioder())));
    }
}
