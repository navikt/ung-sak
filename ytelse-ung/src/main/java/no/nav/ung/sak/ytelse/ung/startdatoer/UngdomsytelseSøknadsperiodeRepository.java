package no.nav.ung.sak.ytelse.ung.startdatoer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class UngdomsytelseSøknadsperiodeRepository {

    private final EntityManager entityManager;

    @Inject
    public UngdomsytelseSøknadsperiodeRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<UngdomsytelseSøknadGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, List<UngdomsytelseSøktStartdato> søknadsperioder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UngdomsytelseSøknadGrunnlag(behandlingId, it))
            .orElse(new UngdomsytelseSøknadGrunnlag(behandlingId));
        nyttGrunnlag.leggTil(søknadsperioder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    public void lagreRelevanteSøknader(Long behandlingId, UngdomsytelseSøknader søknader) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = eksisterendeGrunnlag.map(it -> new UngdomsytelseSøknadGrunnlag(behandlingId, it))
            .orElse(new UngdomsytelseSøknadGrunnlag(behandlingId));
        nyttGrunnlag.setRelevanteSøknader(søknader);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }



    private void persister(Optional<UngdomsytelseSøknadGrunnlag> eksisterendeGrunnlag, UngdomsytelseSøknadGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        if (nyttGrunnlag.getOppgitteSøknader() != null) {
            entityManager.persist(nyttGrunnlag.getOppgitteSøknader());
        }
        if (nyttGrunnlag.getRelevantSøknader() != null) {
            entityManager.persist(nyttGrunnlag.getRelevantSøknader());
        }
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(UngdomsytelseSøknadGrunnlag gr) {
        gr.setAktiv(false);
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<UngdomsytelseSøknadGrunnlag> hentEksisterendeGrunnlag(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM UngdomsytelseSøknadsperiodeGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", UngdomsytelseSøknadGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        eksisterendeGrunnlag.ifPresent(entitet -> {
            persister(Optional.empty(), new UngdomsytelseSøknadGrunnlag(nyBehandlingId, entitet));
        });
    }
}
