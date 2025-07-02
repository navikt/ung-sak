package no.nav.ung.sak.behandlingslager.behandling.søknad;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@Dependent
public class SøknadRepository {

    private EntityManager entityManager;

    protected SøknadRepository() {
    }

    @Inject
    public SøknadRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    /** @return Søknad (hvis eksisterer) eller null. */
    public SøknadEntitet hentSøknad(Behandling behandling) {
        Long behandlingId = behandling.getId();
        if (behandlingId == null) {
            return null;
        }
        return hentSøknad(behandlingId);
    }

    public SøknadEntitet hentSøknad(Long behandlingId) {
        return hentSøknadHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Kunne ikke finne søknad tilknyttet behandling: " + behandlingId));
    }

    public Optional<SøknadEntitet> hentSøknadHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        return hentEksisterendeGrunnlag(behandlingId).map(SøknadGrunnlagEntitet::getSøknad);
    }

    public void lagreOgFlush(Behandling behandling, SøknadEntitet søknad) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$
        Long behandlingId = behandling.getId();
        lagreOgFlush(behandlingId, søknad);
    }

    public void lagreOgFlush(Long behandlingId, SøknadEntitet søknad) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Optional<SøknadGrunnlagEntitet> søknadGrunnlagEntitet = hentEksisterendeGrunnlag(behandlingId);
        if (søknadGrunnlagEntitet.isPresent()) {
            // deaktiver eksisterende grunnlag
            var eksisterende = søknadGrunnlagEntitet.get();
            eksisterende.setAktiv(false);
            entityManager.persist(eksisterende);
            entityManager.flush();
        }

        var grunnlagEntitet = new SøknadGrunnlagEntitet(behandlingId, søknad);
        entityManager.persist(søknad);
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<SøknadGrunnlagEntitet> hentEksisterendeGrunnlag(Long behandlingId) {
        final TypedQuery<SøknadGrunnlagEntitet> query = entityManager.createQuery(
            "FROM SøknadGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
            SøknadGrunnlagEntitet.class);

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

    @SuppressWarnings("unchecked")
    public Optional<Long> hentBehandlingIdForSisteMottattSøknad(Long fagsakId) {
        Query query = entityManager.createNativeQuery(""
            + "select gr.behandling_id from GR_SOEKNAD gr "
            + " inner join SO_SOEKNAD so ON so.id = gr.soeknad_id "
            + " inner join BEHANDLING b on b.id = gr.behandling_id "
            + " where b.fagsak_id = :fagsakId"
            + "   AND gr.aktiv=TRUE"
            + " order by so.mottatt_dato desc");
        query.setMaxResults(1);
        query.setParameter("fagsakId", fagsakId);

        return query.getResultStream().findFirst().map(v -> Long.valueOf(((Number) v).longValue()));
    }

    @SuppressWarnings("unchecked")
    public List<Behandling> hentBehandlingerMedOverlappendeSøknaderIPeriode(Long fagsakId, LocalDate pFom, LocalDate pTom) {
        Objects.requireNonNull(pFom, "pFom");
        Objects.requireNonNull(pTom, "pTom");
        Query query = entityManager.createNativeQuery(""
            + "select gr.behandling_id from GR_SOEKNAD gr "
            + " inner join SO_SOEKNAD so ON so.id = gr.soeknad_id "
            + " inner join BEHANDLING b on b.id = gr.behandling_id "
            + " where b.fagsak_id = :fagsakId"
            + "   AND so.startdato <= :pTom AND so.startdato >= :pFom"
            + "   AND gr.aktiv=TRUE");
        query.setParameter("fagsakId", fagsakId);
        query.setParameter("pFom", pFom);
        query.setParameter("pTom", pTom);

        List<Long> behandlingIder = (List<Long>) query.getResultStream().map(v -> Long.valueOf(((Number) v).longValue())).collect(Collectors.<Long> toList());

        Query queryBeh = entityManager.createQuery("from Behandling where id in (:ids)")
            .setParameter("ids", behandlingIder);

        return queryBeh.getResultList();

    }

    /** Returnerer alle søknader for fagsak, i rekkefølge mottatt dato. */
    @SuppressWarnings("unchecked")
    public List<SøknadEntitet> hentSøknaderForFagsak(Long fagsakId) {
        Query query = entityManager.createNativeQuery(""
            + "select distinct so.id from GR_SOEKNAD gr "
            + " inner join SO_SOEKNAD so ON so.id = gr.soeknad_id "
            + " inner join BEHANDLING b on b.id = gr.behandling_id "
            + " where b.fagsak_id = :fagsakId"
            + "   AND gr.aktiv=TRUE");
        query.setParameter("fagsakId", fagsakId);

        List<Long> søknadIder = (List<Long>) query.getResultStream().map(v -> Long.valueOf(((Number) v).longValue())).collect(Collectors.<Long> toList());

        Query queryBeh = entityManager.createQuery("from Søknad where id in (:ids)")
            .setParameter("ids", søknadIder);

        return ((Stream<SøknadEntitet>) queryBeh.getResultStream()).sorted(Comparator.comparing(SøknadEntitet::getMottattDato)).collect(Collectors.toList());

    }
}
