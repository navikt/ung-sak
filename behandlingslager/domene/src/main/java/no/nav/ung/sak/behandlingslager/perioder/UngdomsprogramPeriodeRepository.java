package no.nav.ung.sak.behandlingslager.perioder;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;
import no.nav.ung.sak.trigger.ProsessTriggere;

@Dependent
public class UngdomsprogramPeriodeRepository {

    private final EntityManager entityManager;

    @Inject
    public UngdomsprogramPeriodeRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<UngdomsprogramPeriodeGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public Optional<UngdomsprogramPeriodeGrunnlag> hentGrunnlagFraGrunnlagsReferanse(UUID grunnlagsReferanse) {
        return hentEksisterendeGrunnlag(grunnlagsReferanse);
    }

    public void lagre(Long behandlingId, Collection<UngdomsprogramPeriode> ungdomsprogramPerioder) {
        var nyttGrunnlag = new UngdomsprogramPeriodeGrunnlag(behandlingId);
        nyttGrunnlag.leggTil(ungdomsprogramPerioder);

        persister(hentEksisterendeGrunnlag(behandlingId), nyttGrunnlag);
    }

    public void kopier(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        final var aktivt = hentEksisterendeGrunnlag(nyBehandlingId);
        aktivt.ifPresent(it -> {
            it.setAktiv(false);
            entityManager.persist(it);
            entityManager.flush();
        });
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(eksisterendeBehandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UngdomsprogramPeriodeGrunnlag(nyBehandlingId, it));
        nyttGrunnlag.ifPresent(gr -> persister(Optional.empty(), gr));
    }

    private void persister(Optional<UngdomsprogramPeriodeGrunnlag> eksisterendeGrunnlag, UngdomsprogramPeriodeGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        if (nyttGrunnlag.getUngdomsprogramPerioder() != null) {
            entityManager.persist(nyttGrunnlag.getUngdomsprogramPerioder());
        }

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UngdomsprogramPeriodeGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<UngdomsprogramPeriodeGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "SELECT gr " +
                "FROM UngdomsprogramPeriodeGrunnlag gr " +
                "WHERE gr.behandlingId = :behandlingId " +
                "AND gr.aktiv = true", UngdomsprogramPeriodeGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<UngdomsprogramPeriodeGrunnlag> hentEksisterendeGrunnlag(UUID grunnlagsReferanse) {
        var query = entityManager.createQuery(
            "SELECT gr " +
                "FROM UngdomsprogramPeriodeGrunnlag gr " +
                "WHERE gr.grunnlagsreferanse = :grunnlagsReferanse", UngdomsprogramPeriodeGrunnlag.class);

        query.setParameter("grunnlagsReferanse", grunnlagsReferanse);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<UngdomsprogramPeriodeGrunnlag> hentGrunnlagBasertPåId(Long id) {
        var query = entityManager.createQuery(
            "SELECT gr " +
                "FROM UngdomsprogramPeriodeGrunnlag gr " +
                "WHERE gr.id = :id", UngdomsprogramPeriodeGrunnlag.class);

        query.setParameter("id", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public DiffResult diffResultat(EndringsresultatDiff idEndring, boolean kunSporedeEndringer) {
        var grunnlagId1 = (Long) idEndring.getGrunnlagId1();
        var grunnlagId2 = (Long) idEndring.getGrunnlagId2();
        var grunnlag1 = hentGrunnlagBasertPåId(grunnlagId1)
            .orElse(null);
        var grunnlag2 = hentGrunnlagBasertPåId(grunnlagId2)
            .orElseThrow(() -> new IllegalStateException("id2 ikke kjent"));
        return diff(kunSporedeEndringer, grunnlag1, grunnlag2);
    }

    DiffResult diff(boolean kunSporedeEndringer, UngdomsprogramPeriodeGrunnlag grunnlag1, UngdomsprogramPeriodeGrunnlag grunnlag2) {
        return new RegisterdataDiffsjekker(kunSporedeEndringer).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId)
            .map(grunnlag -> EndringsresultatSnapshot.medSnapshot(UngdomsprogramPeriodeGrunnlag.class, grunnlag.getId()))
            .orElse(EndringsresultatSnapshot.utenSnapshot(UngdomsprogramPeriodeGrunnlag.class));
    }
}
