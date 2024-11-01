package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.ytelse.ung.beregning.UngdomsytelseGrunnlag;

@Dependent
public class UngdomsytelseSøknadsperiodeRepository {

    private final EntityManager entityManager;

    @Inject
    public UngdomsytelseSøknadsperiodeRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<UngdomsytelseSøknadsperiodeGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, List<UngdomsytelseSøknadsperiode> søknadsperioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UngdomsytelseSøknadsperiodeGrunnlag(behandlingId, it))
            .orElse(new UngdomsytelseSøknadsperiodeGrunnlag(behandlingId));
        nyttGrunnlag.leggTil(søknadsperioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    public void lagreRelevanteSøknadsperioder(Long behandlingId, UngdomsytelseSøknadsperioder søknadsperioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UngdomsytelseSøknadsperiodeGrunnlag(behandlingId, it))
            .orElse(new UngdomsytelseSøknadsperiodeGrunnlag(behandlingId));
        nyttGrunnlag.setRelevanteSøknadsperioder(søknadsperioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }



    private void persister(Optional<UngdomsytelseSøknadsperiodeGrunnlag> eksisterendeGrunnlag, UngdomsytelseSøknadsperiodeGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        if (nyttGrunnlag.getOppgitteSøknadsperioder() != null) {
            entityManager.persist(nyttGrunnlag.getOppgitteSøknadsperioder());
        }
        if (nyttGrunnlag.getRelevantSøknadsperioder() != null) {
            entityManager.persist(nyttGrunnlag.getRelevantSøknadsperioder());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UngdomsytelseSøknadsperiodeGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<UngdomsytelseSøknadsperiodeGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UngdomsytelseSøknadsperiodeGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", UngdomsytelseSøknadsperiodeGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        eksisterendeGrunnlag.ifPresent(entitet -> {
            persister(Optional.empty(), new UngdomsytelseSøknadsperiodeGrunnlag(nyBehandlingId, entitet));
        });
    }
}
