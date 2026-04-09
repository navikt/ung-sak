package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Dependent
public class OppgittForutgåendeMedlemskapRepository {

    private final EntityManager entityManager;

    @Inject
    public OppgittForutgåendeMedlemskapRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public OppgittForutgåendeMedlemskapGrunnlag hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g " +
                "FROM OppgittForutgåendeMedlemskapGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId " +
                "AND g.aktiv = true", OppgittForutgåendeMedlemskapGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentEksaktResultat(query);
    }

    public Optional<OppgittForutgåendeMedlemskapGrunnlag> hentGrunnlagHvisEksisterer(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, LocalDate forutgåendePeriodeFom, LocalDate forutgåendePeriodeTom, Set<OppgittBosted> bosteder) {
        var eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        var nyttGrunnlag = new OppgittForutgåendeMedlemskapGrunnlag(behandlingId, forutgåendePeriodeFom, forutgåendePeriodeTom, bosteder);

        persister(eksisterendeGrunnlag, nyttGrunnlag);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentEksisterendeGrunnlag(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new OppgittForutgåendeMedlemskapGrunnlag(nyBehandlingId, eksisterende);
            persister(Optional.empty(), nyttGrunnlag);
        });
    }

    private void persister(Optional<OppgittForutgåendeMedlemskapGrunnlag> eksisterendeGrunnlag, OppgittForutgåendeMedlemskapGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(OppgittForutgåendeMedlemskapGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }

    private Optional<OppgittForutgåendeMedlemskapGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g " +
                "FROM OppgittForutgåendeMedlemskapGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId " +
                "AND g.aktiv = true", OppgittForutgåendeMedlemskapGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }
}
