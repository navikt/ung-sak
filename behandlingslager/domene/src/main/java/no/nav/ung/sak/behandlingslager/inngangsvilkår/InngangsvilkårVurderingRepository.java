package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Dependent
public class InngangsvilkårVurderingRepository {

    private final EntityManager entityManager;

    @Inject
    public InngangsvilkårVurderingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Optional<InngangsvilkårVurderingGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagreBistandsVurderinger(Long behandlingId, List<BistandsvilkårVurderingPeriode> vurderinger) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        var nyHolder = new BistandsvilkårVurderingHolder(vurderinger);
        var livsoppholdHolder = eksisterende.flatMap(InngangsvilkårVurderingGrunnlag::getAndreLivsoppholdsytelserVurderingHolder).orElse(null);
        var nyttGrunnlag = new InngangsvilkårVurderingGrunnlag(behandlingId, nyHolder, livsoppholdHolder);
        persister(eksisterende, nyttGrunnlag);
    }

    public void lagreLivsoppholdsVurderinger(Long behandlingId, List<AndreLivsoppholdsytelserVurderingPeriode> vurderinger) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        var bistandHolder = eksisterende.flatMap(InngangsvilkårVurderingGrunnlag::getBistandsvilkårVurderingHolder).orElse(null);
        var nyHolder = new AndreLivsoppholdsytelserVurderingHolder(vurderinger);
        var nyttGrunnlag = new InngangsvilkårVurderingGrunnlag(behandlingId, bistandHolder, nyHolder);
        persister(eksisterende, nyttGrunnlag);
    }

    public void kopier(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        hentEksisterendeGrunnlag(eksisterendeBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new InngangsvilkårVurderingGrunnlag(
                nyBehandlingId,
                eksisterende.getBistandsvilkårVurderingHolder().orElse(null),
                eksisterende.getAndreLivsoppholdsytelserVurderingHolder().orElse(null)
            );
            persister(Optional.empty(), nyttGrunnlag);
        });
    }

    private void persister(Optional<InngangsvilkårVurderingGrunnlag> eksisterendeGrunnlag, InngangsvilkårVurderingGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(InngangsvilkårVurderingGrunnlag grunnlag) {
        grunnlag.deaktiver();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    private Optional<InngangsvilkårVurderingGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM InngangsvilkårVurderingGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId " +
                "AND g.aktiv = true",
            InngangsvilkårVurderingGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
