package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class VurdertOpplæringRepository {

    private EntityManager entityManager;

    public VurdertOpplæringRepository() {
    }

    @Inject
    public VurdertOpplæringRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<VurdertOpplæringGrunnlag> hentAktivtGrunnlagForBehandling(Long behandlingId) {
        return getAktivtGrunnlag(behandlingId);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long originalBehandlingId, Long nyBehandlingId) {
        Objects.requireNonNull(originalBehandlingId, "originalBehandlingId");
        Objects.requireNonNull(nyBehandlingId, "nyBehandlingId");

        Optional<VurdertOpplæringGrunnlag> aktivtGrunnlagFraForrigeBehandling = getAktivtGrunnlag(originalBehandlingId);
        aktivtGrunnlagFraForrigeBehandling.ifPresent(aktivtGrunnlag -> {
            VurdertOpplæringGrunnlag kopiertGrunnlag = new VurdertOpplæringGrunnlag(nyBehandlingId, aktivtGrunnlag);
            entityManager.persist(kopiertGrunnlag);
            entityManager.flush();
        });
    }

    public void lagre(Long behandlingId, VurdertInstitusjonHolder vurdertInstitusjonHolder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(vurdertInstitusjonHolder, "vurdertInstitusjonHolder");

        var aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        var nyttGrunnlag = new VurdertOpplæringGrunnlag(behandlingId,
            vurdertInstitusjonHolder,
            aktivtGrunnlag.map(VurdertOpplæringGrunnlag::getVurdertOpplæringHolder).orElse(null),
            aktivtGrunnlag.map(VurdertOpplæringGrunnlag::getVurdertePerioder).orElse(null));
        deaktiverEksisterendeGrunnlag(aktivtGrunnlag);

        entityManager.persist(nyttGrunnlag.getVurdertInstitusjonHolder());
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    public void lagre(Long behandlingId, VurdertOpplæringHolder vurdertOpplæringHolder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(vurdertOpplæringHolder, "vurdertOpplæringHolder");

        var aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        var nyttGrunnlag = new VurdertOpplæringGrunnlag(behandlingId,
            aktivtGrunnlag.map(VurdertOpplæringGrunnlag::getVurdertInstitusjonHolder).orElse(null),
            vurdertOpplæringHolder,
            aktivtGrunnlag.map(VurdertOpplæringGrunnlag::getVurdertePerioder).orElse(null));
        deaktiverEksisterendeGrunnlag(aktivtGrunnlag);

        entityManager.persist(nyttGrunnlag.getVurdertOpplæringHolder());
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    public void lagre(Long behandlingId, VurdertOpplæringPerioderHolder vurdertOpplæringPerioderHolder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(vurdertOpplæringPerioderHolder, "vurdertOpplæringPerioderHolder");

        var aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        var nyttGrunnlag = new VurdertOpplæringGrunnlag(behandlingId,
            aktivtGrunnlag.map(VurdertOpplæringGrunnlag::getVurdertInstitusjonHolder).orElse(null),
            aktivtGrunnlag.map(VurdertOpplæringGrunnlag::getVurdertOpplæringHolder).orElse(null),
            vurdertOpplæringPerioderHolder);
        deaktiverEksisterendeGrunnlag(aktivtGrunnlag);

        entityManager.persist(nyttGrunnlag.getVurdertePerioder());
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterendeGrunnlag(Optional<VurdertOpplæringGrunnlag> aktivtGrunnlag) {
        if (aktivtGrunnlag.isPresent()) {
            var vurdertOpplæringGrunnlag = aktivtGrunnlag.get();
            vurdertOpplæringGrunnlag.setAktiv(false);
            entityManager.persist(vurdertOpplæringGrunnlag);
            entityManager.flush();
        }
    }

    private Optional<VurdertOpplæringGrunnlag> getAktivtGrunnlag(Long behandlingId) {
        TypedQuery<VurdertOpplæringGrunnlag> query = entityManager.createQuery(
                "SELECT vog FROM VurdertOpplæringGrunnlag vog WHERE vog.behandlingId = :behandling_id AND vog.aktiv = true",
                VurdertOpplæringGrunnlag.class)
            .setParameter("behandling_id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }
}
