package no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsynBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæringer;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

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

    public PleiebehovResultat hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<PleiebehovResultat> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagreOgFlush(Behandling behandling, PleiebehovBuilder pleiebehovBuilder) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(pleiebehovBuilder, "pleiebehovBuilder"); // NOSONAR $NON-NLS-1$
        final Optional<PleiebehovResultat> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandling.getId());
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final PleiebehovResultat eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final var tilsyn = pleiebehovBuilder.build();
        final PleiebehovResultat grunnlagEntitet = new PleiebehovResultat(behandling, tilsyn);
        entityManager.persist(tilsyn);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<PleiebehovResultat> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<PleiebehovResultat> query = entityManager.createQuery(
            "FROM PleiebehovResultat s " +
                "WHERE s.behandling.id = :behandlingId AND s.aktiv = true", PleiebehovResultat.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling gammelBehandling, Behandling nyBehandling) {
        Optional<PleiebehovResultat> søknadEntitet = hentEksisterendeGrunnlag(gammelBehandling.getId());
        søknadEntitet.ifPresent(entitet -> lagreOgFlush(nyBehandling, new PleiebehovBuilder(entitet.getPleieperioder())));
    }
}
