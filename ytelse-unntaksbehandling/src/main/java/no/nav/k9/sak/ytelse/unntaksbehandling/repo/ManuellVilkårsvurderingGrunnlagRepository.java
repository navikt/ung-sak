package no.nav.k9.sak.ytelse.unntaksbehandling.repo;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class ManuellVilkårsvurderingGrunnlagRepository {

    private EntityManager entityManager;

    @SuppressWarnings("unused")
    ManuellVilkårsvurderingGrunnlagRepository() {
        // CDI
    }

    @Inject
    public ManuellVilkårsvurderingGrunnlagRepository(EntityManager entityManager) {
        requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Optional<ManuellVilkårsvurderingGrunnlag> hentGrunnlag(Long behandlingId) {
        final TypedQuery<ManuellVilkårsvurderingGrunnlag> query =
            entityManager.createQuery(
                "SELECT v FROM ManuellVilkårsvurderingGrunnlag v " +
                    "WHERE v.behandlingId = :behandlingId AND v.aktiv = true",
                ManuellVilkårsvurderingGrunnlag.class
            );

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOgFlushFritekst(Long behandlingId, VilkårsvurderingFritekst input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new ManuellVilkårsvurderingGrunnlag(behandlingId, input));
    }

    private void lagreOgFlushNyttGrunnlag(ManuellVilkårsvurderingGrunnlag grunnlagEntitet) {
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private void deaktiverEksisterendeGrunnlag(ManuellVilkårsvurderingGrunnlag eksisterende) {
        if (eksisterende == null) {
            return;
        }
        eksisterende.setAktiv(false);
        lagreOgFlushNyttGrunnlag(eksisterende);
    }
}
