package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class SøknadRepository {

    private EntityManager entityManager;

    protected SøknadRepository() {
    }

    @Inject
    public SøknadRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public SøknadEntitet hentSøknad(Behandling behandling) {
        Long behandlingId = behandling.getId();
        return hentSøknad(behandlingId);
    }

    public SøknadEntitet hentSøknad(Long behandlingId) {
        if (behandlingId == null) {
            return null;
        }
        return hentSøknadHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<SøknadEntitet> hentSøknadHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        return hentEksisterendeGrunnlag(behandlingId).map(SøknadGrunnlagEntitet::getSøknad);
    }

    public void lagreOgFlush(Behandling behandling, SøknadEntitet søknad) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$
        final Optional<SøknadGrunnlagEntitet> søknadGrunnlagEntitet = hentEksisterendeGrunnlag(behandling.getId());
        if (søknadGrunnlagEntitet.isPresent()) {
            // deaktiver eksisterende grunnlag

            final SøknadGrunnlagEntitet søknadGrunnlagEntitet1 = søknadGrunnlagEntitet.get();
            søknadGrunnlagEntitet1.setAktiv(false);
            entityManager.persist(søknadGrunnlagEntitet1);
            entityManager.flush();
        }

        final SøknadGrunnlagEntitet grunnlagEntitet = new SøknadGrunnlagEntitet(behandling, søknad);
        entityManager.persist(søknad);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<SøknadGrunnlagEntitet> hentEksisterendeGrunnlag(Long behandlingId) {
        final TypedQuery<SøknadGrunnlagEntitet> query = entityManager.createQuery(
            "FROM SøknadGrunnlag s " +
                "WHERE s.behandling.id = :behandlingId AND s.aktiv = true", SøknadGrunnlagEntitet.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling gammelBehandling, Behandling nyBehandling) {
        Optional<SøknadEntitet> søknadEntitet = hentSøknadHvisEksisterer(gammelBehandling.getId());
        søknadEntitet.ifPresent(entitet -> lagreOgFlush(nyBehandling, entitet));
    }
}
