package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@Dependent
public class OmsorgenForGrunnlagRepository {

    private EntityManager entityManager;

    OmsorgenForGrunnlagRepository() {
        // CDI
    }

    @Inject
    public OmsorgenForGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public OmsorgenForGrunnlag hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<OmsorgenForGrunnlag> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    /*
    public void lagreNyePeriode(Long behandlingId, OmsorgenFor omsorgenFor) {
        var omsorgenForGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        lagre(behandlingId, omsorgenFor);
    }
    */

    public void lagre(Long behandlingId, OmsorgenFor omsorgenFor) {
        final Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        lagre(behandlingId, omsorgenFor, eksisterendeGrunnlag);
    }
    
    public void lagre(Long behandlingId, OmsorgenForPeriode omsorgForPeriode) {
        final Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        final OmsorgenFor omsorgenFor = eksisterendeGrunnlag.map(og -> new OmsorgenFor(og.getOmsorgenFor())).orElse(new OmsorgenFor());
        omsorgenFor.addPeriode(omsorgForPeriode);
        
        lagre(behandlingId, omsorgenFor, eksisterendeGrunnlag);
    }
    
    private void lagre(Long behandlingId, OmsorgenFor omsorgenFor, Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag) {
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final OmsorgenForGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final OmsorgenForGrunnlag grunnlagEntitet = new OmsorgenForGrunnlag(behandlingId, omsorgenFor);
        if (omsorgenFor != null) {
            entityManager.persist(omsorgenFor);
        }

        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<OmsorgenForGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<OmsorgenForGrunnlag> query = entityManager.createQuery(
            "FROM OmsorgenForGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", OmsorgenForGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling gammelBehandling, Behandling nyBehandling) {
        kopierGrunnlagFraEksisterendeBehandling(gammelBehandling.getId(), nyBehandling.getId());
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<OmsorgenForGrunnlag> søknadEntitet = hentEksisterendeGrunnlag(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> {
            lagre(nyBehandlingId, new OmsorgenFor(entitet.getOmsorgenFor()));
        });
    }
}
