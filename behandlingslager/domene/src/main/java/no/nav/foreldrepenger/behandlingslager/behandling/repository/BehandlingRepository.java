package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class BehandlingRepository {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private static final String FAGSAK_ID = "fagsakId"; //$NON-NLS-1$
    private static final String BEHANDLING_ID = "behandlingId"; //$NON-NLS-1$
    private static final String BEHANDLING_UUID = "behandlingUuid"; //$NON-NLS-1$

    private EntityManager entityManager;

    BehandlingRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private static Optional<Behandling> optionalFirst(List<Behandling> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.get(0));
    }

    EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Hent Behandling, der det ikke er gitt at behandlingId er korrekt (eks. for validering av innsendte verdier)
     */
    public Optional<Behandling> finnUnikBehandlingForBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR //$NON-NLS-1$
        return hentUniktResultat(lagBehandlingQuery(behandlingId));
    }

    /**
     * Hent Behandling med angitt id.
     */
    public Behandling hentBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR //$NON-NLS-1$
        return hentEksaktResultat(lagBehandlingQuery(behandlingId));
    }

    /**
     * Henter behandling for angitt id (erstatter andre {@link #hentBehandling(Long) og #hentBehandling(UUID)}
     * 
     * @param behandlingId må være type Long eller UUID format
     */
    public Behandling hentBehandling(String behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); //$NON-NLS-1$
        if (DIGITS_PATTERN.matcher(behandlingId).matches()) {
            return hentBehandling(Long.parseLong(behandlingId));
        } else {
            return hentBehandling(UUID.fromString(behandlingId));
        }
    }

    /**
     * Hent Behandling med angitt uuid.
     */
    public Behandling hentBehandling(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR //$NON-NLS-1$
        return hentEksaktResultat(lagBehandlingQuery(behandlingUuid));
    }

    /**
     * Hent Behandling med angitt uuid hvis den finnes.
     */
    public Optional<Behandling> hentBehandlingHvisFinnes(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR //$NON-NLS-1$
        return hentUniktResultat(lagBehandlingQuery(behandlingUuid));
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt saksnummer.
     * Dette er eksternt saksnummer angitt av GSAK.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForSaksnummer(Saksnummer saksnummer) {
        Objects.requireNonNull(saksnummer, "saksnummer"); //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh, Fagsak AS fagsak WHERE beh.fagsak.id=fagsak.id AND fagsak.saksnummer=:saksnummer", //$NON-NLS-1$
            Behandling.class);
        query.setParameter("saksnummer", saksnummer); //$NON-NLS-1$
        return query.getResultList();
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt fagsakId.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForFagsak(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // $NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh, Fagsak AS fagsak WHERE beh.fagsak.id=fagsak.id AND fagsak.id=:fagsakId", //$NON-NLS-1$
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId); // $NON-NLS-1$
        return query.getResultList();
    }

    /**
     * Hent siste behandling for angitt {@link Fagsak#id}
     */
    public Optional<Behandling> hentSisteBehandlingForFagsakId(Long fagsakId) {
        return finnSisteBehandling(fagsakId, false);
    }

    /**
     * Hent siste behandling for angitt {@link Fagsak#id}
     */
    public Optional<Behandling> hentSisteYtelsesBehandlingForFagsakId(Long fagsakId) {
        return finnSisteBehandling(fagsakId, BehandlingType.getYtelseBehandlingTyper(), false);
    }

    /**
     * Hent siste behandling for angitt {@link Fagsak#id} og behandling type
     */
    public Optional<Behandling> hentSisteBehandlingAvBehandlingTypeForFagsakId(Long fagsakId, BehandlingType behandlingType) {
        return finnSisteBehandling(fagsakId, Set.of(behandlingType), false);
    }

    /**
     * Hent alle behandlinger for en fagsak som har en av de angitte behandlingsårsaker
     */
    public List<Behandling> hentBehandlingerMedÅrsakerForFagsakId(Long fagsakId, Set<BehandlingÅrsakType> årsaker) {
        TypedQuery<Behandling> query = getEntityManager().createQuery("SELECT b FROM Behandling b" +
            " WHERE b.fagsak.id = :fagsakId " +
            " AND EXISTS (SELECT å FROM BehandlingÅrsak å" +
            "   WHERE å.behandling = b AND å.behandlingÅrsakType IN :årsaker)", Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("årsaker", årsaker);

        return query.getResultList();
    }

    /**
     * Hent alle behandlinger som ikke er avsluttet på fagsak.
     */
    public List<Behandling> hentBehandlingerSomIkkeErAvsluttetForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // $NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh WHERE beh.fagsak.id = :fagsakId AND beh.status != :status", //$NON-NLS-1$
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId); // $NON-NLS-1$
        query.setParameter("status", BehandlingStatus.AVSLUTTET); // NOSONAR //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    /**
     * Hent alle åpne behandlinger på fagsak.
     */
    public List<Behandling> hentÅpneBehandlingerForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // $NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.status NOT IN (:status)", //$NON-NLS-1$
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId); // $NON-NLS-1$
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser()); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    public List<Long> hentÅpneBehandlingerIdForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // $NON-NLS-1$

        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT beh.id from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.status NOT IN (:status)", //$NON-NLS-1$
            Long.class);
        query.setParameter(FAGSAK_ID, fagsakId); // $NON-NLS-1$
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser()); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_CACHE_MODE, "IGNORE");
        return query.getResultList();
    }

    /**
     * Kaller lagre Behandling, og renser first-level cache i JPA.
     */
    public Long lagreOgClear(Behandling behandling, BehandlingLås lås) {
        Long id = lagre(behandling, lås);
        getEntityManager().clear();
        return id;
    }

    /**
     * Lagrer behandling, sikrer at relevante parent-entiteter (Fagsak, FagsakRelasjon) også oppdateres.
     */
    public Long lagre(Behandling behandling, BehandlingLås lås) {
        if (!Objects.equals(behandling.getId(), lås.getBehandlingId())) {
            // hvis satt må begge være like. (Objects.equals håndterer også at begge er null)
            throw new IllegalArgumentException(
                "Behandling#id [" + behandling.getId() + "] og lås#behandlingId [" + lås.getBehandlingId() + "] må være like, eller begge må være null."); //$NON-NLS-1$
        }

        long behandlingId = lagre(behandling);
        verifiserBehandlingLås(lås);

        // i tilfelle denne ikke er satt fra før, f.eks. for ny entitet
        lås.setBehandlingId(behandlingId);

        return behandlingId;
    }

    public Optional<Behandling> finnSisteAvsluttedeIkkeHenlagteBehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        return optionalFirst(finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsakId));
    }

    public List<Behandling> finnAlleAvsluttedeIkkeHenlagteBehandlinger(Long fagsakId) {
        // BehandlingResultatType = Innvilget, endret, ikke endret, avslått.
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // NOSONAR //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT behandling FROM Behandling behandling " +
                "INNER JOIN Behandlingsresultat behandlingsresultat ON behandling=behandlingsresultat.behandling " +
                "INNER JOIN BehandlingVedtak behandling_vedtak ON behandlingsresultat=behandling_vedtak.behandlingsresultat " +
                "WHERE behandling.status IN :avsluttetOgIverkKode " +
                "  AND behandling.fagsak.id=:fagsakId " +
                "ORDER BY behandling_vedtak.vedtakstidspunkt DESC, behandling_vedtak.endretTidspunkt DESC",
            Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("avsluttetOgIverkKode", BehandlingStatus.getFerdigbehandletStatuser());
        query.setHint(QueryHints.HINT_READONLY, true);
        return query.getResultList();
    }

    public Optional<Behandling> finnSisteInnvilgetBehandling(Long fagsakId) {
        // BehandlingResultatType = Innvilget, endret.
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT behandling FROM Behandling behandling " +
                "INNER JOIN Behandlingsresultat behandlingsresultat ON behandling=behandlingsresultat.behandling " +
                "INNER JOIN BehandlingVedtak behandling_vedtak ON behandlingsresultat=behandling_vedtak.behandlingsresultat " +
                "WHERE behandling.status IN :avsluttetOgIverkKode " +
                "  AND behandlingsresultat.behandlingResultatType IN (:innvilgetKoder) " +
                "  AND behandling.fagsak.id=:fagsakId " +
                "ORDER BY behandling_vedtak.vedtakstidspunkt DESC, behandling_vedtak.endretTidspunkt DESC",
            Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("avsluttetOgIverkKode", BehandlingStatus.getFerdigbehandletStatuser());
        query.setParameter("innvilgetKoder", BehandlingResultatType.getInnvilgetKoder());

        return optionalFirst(query.getResultList());
    }

    /**
     * Ta lås for oppdatering av behandling/fagsak. Påkrevd før lagring.
     * Convenience metode som tar hele entiteten.
     *
     * @see #taSkriveLås(Long, Long)
     */
    public BehandlingLås taSkriveLås(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling"); //$NON-NLS-1$
        Long behandlingId = behandling.getId();
        return taSkriveLås(behandlingId);
    }

    public BehandlingLås taSkriveLås(Long behandlingId) {
        BehandlingLåsRepository låsRepo = new BehandlingLåsRepository(getEntityManager());
        return låsRepo.taLås(behandlingId);
    }

    private Optional<Behandling> finnSisteBehandling(Long fagsakId, Set<BehandlingType> behandlingType, boolean readOnly) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        Objects.requireNonNull(behandlingType, "behandlingType");

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "from Behandling where fagsak.id=:fagsakId and behandlingType in (:behandlingType) order by opprettetTidspunkt desc",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("behandlingType", behandlingType);
        if (readOnly) {
            query.setHint(QueryHints.HINT_READONLY, "true");
        }
        return optionalFirst(query.getResultList());
    }

    private Optional<Behandling> finnSisteBehandling(Long fagsakId, boolean readOnly) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "from Behandling where fagsak.id=:fagsakId order by opprettetTidspunkt desc",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        if (readOnly) {
            query.setHint(QueryHints.HINT_READONLY, "true");
        }
        return optionalFirst(query.getResultList());
    }

    public Optional<Behandling> finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(Long fagsakId, BehandlingType behandlingType) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        Objects.requireNonNull(behandlingType, "behandlingType");

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            " FROM Behandling b WHERE b.fagsak.id=:fagsakId " +
                " AND b.behandlingType=:behandlingType " +
                " AND NOT EXISTS (SELECT r FROM Behandlingsresultat r" +
                "    WHERE r.behandling=b " +
                "    AND r.behandlingResultatType IN :henlagtKoder)" +
                " ORDER BY b.opprettetTidspunkt DESC",
            Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("behandlingType", behandlingType);
        query.setParameter("henlagtKoder", BehandlingResultatType.getAlleHenleggelseskoder());

        return optionalFirst(query.getResultList());
    }

    private IllegalStateException flereAggregatOpprettelserISammeLagringException(Class<?> aggregat) {
        return new IllegalStateException("Glemt å lagre "
            + aggregat.getSimpleName()
            + "? Denne må lagres separat siden den er et selvstendig aggregat delt mellom behandlinger"); //$NON-NLS-1$
    }

    private TypedQuery<Behandling> lagBehandlingQuery(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery("from Behandling where id=:" + BEHANDLING_ID, Behandling.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_ID, behandlingId); // $NON-NLS-1$
        return query;
    }

    private TypedQuery<Behandling> lagBehandlingQuery(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR //$NON-NLS-1$

        TypedQuery<Behandling> query = getEntityManager().createQuery("from Behandling where uuid=:" + BEHANDLING_UUID, Behandling.class); //$NON-NLS-1$
        query.setParameter(BEHANDLING_UUID, behandlingUuid); // $NON-NLS-1$
        return query;
    }

    private Long lagre(Vilkårene vilkårene) {
        getEntityManager().persist(vilkårene);
        for (Vilkår vilkår : vilkårene.getVilkårene()) {
            getEntityManager().persist(vilkår);
            for (VilkårPeriode vilkårPeriode : vilkår.getPerioder()) {
                getEntityManager().persist(vilkårPeriode);
            }
        }
        return vilkårene.getId();
    }

    /**
     * sjekk lås og oppgrader til skriv
     */
    public void verifiserBehandlingLås(BehandlingLås lås) {
        BehandlingLåsRepository låsHåndterer = new BehandlingLåsRepository(getEntityManager());
        låsHåndterer.oppdaterLåsVersjon(lås);
    }

    Long lagre(Behandling behandling) {
        getEntityManager().persist(behandling);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        if (behandlingsresultat != null) {
            getEntityManager().persist(behandlingsresultat);
        }
        List<BehandlingÅrsak> behandlingÅrsak = behandling.getBehandlingÅrsaker();
        behandlingÅrsak.forEach(getEntityManager()::persist);

        getEntityManager().flush();

        return behandling.getId();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    @SuppressWarnings("unchecked")
    public Long hentEksisterendeVersjon(Long behandlingId) {
        Query query = getEntityManager().createNativeQuery(
            "SELECT behandling.versjon FROM behandling WHERE behandling.id = ?");
        query.setParameter(1, behandlingId);
        return (Long) query.getResultStream().findFirst().orElse(null);
    }

    public void oppdaterSistOppdatertTidspunkt(Behandling behandling, LocalDateTime tidspunkt) {
        Query query = getEntityManager().createNativeQuery("UPDATE BEHANDLING SET SIST_OPPDATERT_TIDSPUNKT = :tidspunkt WHERE " +
            "ID = :behandling_id");

        query.setParameter("tidspunkt", tidspunkt); // NOSONAR $NON-NLS-1$
        query.setParameter("behandling_id", behandling.getId()); // NOSONAR $NON-NLS-1$

        query.executeUpdate();
    }

    public Optional<LocalDateTime> hentSistOppdatertTidspunkt(Long behandlingId) {
        Query query = getEntityManager().createNativeQuery("SELECT be.SIST_OPPDATERT_TIDSPUNKT FROM BEHANDLING be WHERE be.ID = :behandling_id");

        query.setParameter("behandling_id", behandlingId); // NOSONAR $NON-NLS-1$

        Object resultat = query.getSingleResult();
        if (resultat == null) {
            return Optional.empty();
        }

        Timestamp timestamp = (Timestamp) resultat;
        LocalDateTime value = LocalDateTime.ofInstant(timestamp.toInstant(), TimeZone.getDefault().toZoneId());
        return Optional.of(value);
    }

    public List<BehandlingÅrsak> finnÅrsakerForBehandling(Behandling behandling) {
        TypedQuery<BehandlingÅrsak> query = entityManager.createQuery(
            "FROM BehandlingÅrsak  årsak " +
                "WHERE (årsak.behandling = :behandling)",
            BehandlingÅrsak.class);

        query.setParameter("behandling", behandling);
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }

    public List<BehandlingÅrsakType> finnÅrsakTyperForBehandling(Behandling behandling) {
        TypedQuery<BehandlingÅrsakType> query = entityManager.createQuery(
            "select distinct behandlingÅrsakType FROM BehandlingÅrsak årsak " +
                "WHERE årsak.behandling = :behandling",
            BehandlingÅrsakType.class);

        query.setParameter("behandling", behandling);
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return query.getResultList();
    }
}
