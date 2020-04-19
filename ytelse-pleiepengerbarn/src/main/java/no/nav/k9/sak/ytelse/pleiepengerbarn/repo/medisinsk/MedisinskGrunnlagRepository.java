package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
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

    public void lagre(Long behandlingId, OmsorgenFor omsorgenFor) {
        var medisinskGrunnlag = hentEksisterendeGrunnlag(behandlingId);

        var legeerklæringer = medisinskGrunnlag.map(MedisinskGrunnlag::getLegeerklæringer).orElse(null);
        var kontinuerligTilsyn = medisinskGrunnlag.map(MedisinskGrunnlag::getKontinuerligTilsyn).orElse(null);
        lagre(behandlingId, kontinuerligTilsyn, legeerklæringer, omsorgenFor);
    }

    public void lagre(Long behandlingId, KontinuerligTilsynBuilder kontinuerligTilsyn, Legeerklæringer legeerklæringer) {
        Objects.requireNonNull(behandlingId, "behandling"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(kontinuerligTilsyn, "kontinuerligTilsyn"); // NOSONAR $NON-NLS-1$

        var medisinskGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var omsorgenFor = medisinskGrunnlag.map(MedisinskGrunnlag::getOmsorgenFor).orElse(null);

        lagre(behandlingId, kontinuerligTilsyn.build(), legeerklæringer, omsorgenFor);
    }

    private void lagre(Long behandlingId, KontinuerligTilsyn kontinuerligTilsyn, Legeerklæringer legeerklæringer, OmsorgenFor omsorgenFor) {

        final Optional<MedisinskGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final MedisinskGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final MedisinskGrunnlag grunnlagEntitet = new MedisinskGrunnlag(behandlingId, kontinuerligTilsyn, legeerklæringer, omsorgenFor);
        if (kontinuerligTilsyn != null) {
            entityManager.persist(kontinuerligTilsyn);
        }
        if (legeerklæringer != null) {
            entityManager.persist(legeerklæringer);
        }
        if (omsorgenFor != null) {
            entityManager.persist(omsorgenFor);
        }
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<MedisinskGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<MedisinskGrunnlag> query = entityManager.createQuery(
            "FROM MedisinskGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", MedisinskGrunnlag.class);

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
        Optional<MedisinskGrunnlag> søknadEntitet = hentEksisterendeGrunnlag(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> {
            lagre(nyBehandlingId, new KontinuerligTilsyn(entitet.getKontinuerligTilsyn()),
                entitet.getLegeerklæringer(), entitet.getOmsorgenFor());
        });
    }
}
