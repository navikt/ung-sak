package no.nav.ung.sak.behandlingslager.behandling.repository;

import static no.nav.k9.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.k9.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.jpa.QueryHints;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.Saksnummer;

@Dependent
public class BehandlingRepository {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private static final String FAGSAK_ID = "fagsakId";
    private static final String BEHANDLING_ID = "behandlingId";
    private static final String BEHANDLING_UUID = "behandlingUuid";

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
    public Optional<Behandling> hentBehandlingHvisFinnes(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR
        return medAktiveBehandlingTilstanderFilter(() ->
            hentUniktResultat(lagBehandlingQuery(behandlingId)));
    }

    /**
     * Hent Behandling med angitt id.
     */
    public Behandling hentBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR
        return medAktiveBehandlingTilstanderFilter(() ->
            hentEksaktResultat(lagBehandlingQuery(behandlingId)));
    }

    /**
     * Henter behandling for angitt id (erstatter andre {@link #hentBehandling(Long) og #hentBehandling(UUID)}
     *
     * @param behandlingId må være type Long eller UUID format
     */
    public Behandling hentBehandling(String behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
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
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR
        return medAktiveBehandlingTilstanderFilter( () ->
            hentEksaktResultat(lagBehandlingQuery(behandlingUuid)));
    }

    /**
     * Hent Behandling med angitt uuid hvis den finnes.
     */
    public Optional<Behandling> hentBehandlingHvisFinnes(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR
        return medAktiveBehandlingTilstanderFilter( () ->
            hentUniktResultat(lagBehandlingQuery(behandlingUuid)));
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt saksnummer.
     * Dette er eksternt saksnummer angitt av GSAK.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForSaksnummer(Saksnummer saksnummer) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(saksnummer.getVerdi());

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh JOIN FETCH beh.fagsak WHERE beh.fagsak.saksnummer=:saksnummer",
            Behandling.class);
        query.setParameter("saksnummer", saksnummer);
        return medAktiveBehandlingTilstanderFilter(query::getResultList);
    }

    /**
     * NB: Sikker på at du vil hente alle behandlinger, inklusiv de som er lukket?
     * <p>
     * Hent alle behandlinger for angitt fagsakId.
     */
    public List<Behandling> hentAbsoluttAlleBehandlingerForFagsak(Long fagsakId) {

        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh JOIN FETCH beh.fagsak where beh.fagsak.id=:fagsakId",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        return medAktiveBehandlingTilstanderFilter(query::getResultList);
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
     * Hent alle behandlinger som ikke er avsluttet på fagsak.
     */
    public List<Behandling> hentBehandlingerSomIkkeErAvsluttetForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh WHERE beh.fagsak.id = :fagsakId AND beh.status != :status",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("status", BehandlingStatus.AVSLUTTET); // NOSONAR
        query.setHint(QueryHints.HINT_READONLY, "true");
        return medAktiveBehandlingTilstanderFilter(query::getResultList);
    }

    /**
     * Hent alle åpne behandlinger på fagsak.
     */
    public List<Behandling> hentÅpneBehandlingerForFagsakId(Long fagsakId, BehandlingType... behandlingTyper) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        List<BehandlingType> typerList = Arrays.asList(behandlingTyper == null || behandlingTyper.length == 0 ? BehandlingType.values() : behandlingTyper);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.behandlingType IN (:behandlingType)" +
                "AND beh.status NOT IN (:status)",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("behandlingType", typerList);
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser());
        query.setHint(QueryHints.HINT_READONLY, "true");
        return medAktiveBehandlingTilstanderFilter(query::getResultList);
    }

    public List<Long> hentÅpneBehandlingerIdForFagsakId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT beh.id from Behandling AS beh " +
                "WHERE beh.fagsak.id = :fagsakId " +
                "AND beh.status NOT IN (:status)",
            Long.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("status", BehandlingStatus.getFerdigbehandletStatuser());
        query.setHint(QueryHints.HINT_READONLY, "true");
        query.setHint(QueryHints.HINT_CACHE_MODE, "IGNORE");
        return medAktiveBehandlingTilstanderFilter(query::getResultList);
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
                "Behandling#id [" + behandling.getId() + "] og lås#behandlingId [" + lås.getBehandlingId() + "] må være like, eller begge må være null.");
        }

        håndterAksjonspunkt(behandling);

        long behandlingId = lagre(behandling);
        verifiserBehandlingLås(lås);

        // i tilfelle denne ikke er satt fra før, f.eks. for ny entitet
        lås.setBehandlingId(behandlingId);

        return behandlingId;
    }

    private void håndterAksjonspunkt(Behandling behandling) {
        for (var a : behandling.getAksjonspunkter()) {
            if (a.erÅpentAksjonspunkt() && !a.getAksjonspunktDefinisjon().validerGyldigStatusEndring(a.getStatus(), behandling.getStatus())) {
                throw new IllegalStateException("Ugyldig tilstand: Har åpent aksjonspunkt: " + a + " for behandling:" + behandling + ". Aksjonspunktet kan kun være åpent for status:"
                    + a.getAksjonspunktDefinisjon().getGyldigBehandlingStatus());
            }
        }
    }

    public Optional<Behandling> finnSisteAvsluttedeIkkeHenlagteBehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        return optionalFirst(finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsakId));
    }

    public List<Behandling> finnAlleAvsluttedeIkkeHenlagteBehandlinger(Long fagsakId) {
        // BehandlingResultatType = Innvilget, endret, ikke endret, avslått.
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // NOSONAR

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT behandling FROM Behandling behandling " +
                "INNER JOIN BehandlingVedtak behandling_vedtak ON behandling.id=behandling_vedtak.behandlingId " +
                "WHERE behandling.status IN :avsluttetOgIverkKode " +
                "  AND behandling.fagsak.id=:fagsakId " +
                "ORDER BY behandling_vedtak.vedtakstidspunkt DESC, behandling_vedtak.endretTidspunkt DESC",
            Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("avsluttetOgIverkKode", BehandlingStatus.getFerdigbehandletStatuser());
        query.setHint(QueryHints.HINT_READONLY, true);

        // lukker bort henlagte
        List<Behandling> behandlinger = medAktiveBehandlingTilstanderFilter(query::getResultList);
        return behandlinger.stream()
            .filter(b -> !b.getBehandlingResultatType().erHenleggelse())
            .collect(Collectors.toList()); // NB List - må ivareta rekkefølge sortert på tid
    }

    public List<Behandling> finnAlleIkkeHenlagteBehandlinger(Long fagsakId) {
        // BehandlingResultatType = Innvilget, endret, ikke endret, avslått.
        Objects.requireNonNull(fagsakId, FAGSAK_ID); // NOSONAR

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT behandling FROM Behandling behandling " +
                "WHERE behandling.fagsak.id=:fagsakId " +
                "AND behandling.status IN :avsluttetOgIverkKode " +
                "ORDER BY behandling.opprettetTidspunkt DESC",
            Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("avsluttetOgIverkKode", BehandlingStatus.getFerdigbehandletStatuser());
        query.setHint(QueryHints.HINT_READONLY, true);

        // lukker bort henlagte
        List<Behandling> behandlinger = medAktiveBehandlingTilstanderFilter(query::getResultList);
        return behandlinger.stream()
            .filter(b -> !b.getBehandlingResultatType().erHenleggelse())
            .collect(Collectors.toList()); // NB List - må ivareta rekkefølge sortert på tid
    }

    public Optional<Behandling> finnSisteInnvilgetBehandling(Long fagsakId) {
        // BehandlingResultatType = Innvilget, endret.
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT b FROM Behandling b " +
                " INNER JOIN BehandlingVedtak bv ON b.id=bv.behandlingId " +
                " WHERE b.status IN :avsluttetOgIverkKode " +
                "  AND b.behandlingResultatType IN (:innvilgetKoder) " +
                "  AND b.fagsak.id=:fagsakId " +
                " ORDER BY bv.vedtakstidspunkt DESC, bv.endretTidspunkt DESC",
            Behandling.class);

        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("avsluttetOgIverkKode", BehandlingStatus.getFerdigbehandletStatuser());
        query.setParameter("innvilgetKoder", BehandlingResultatType.getInnvilgetKoder());

        return optionalFirst(medAktiveBehandlingTilstanderFilter(query::getResultList));
    }

    /**
     * Ta lås for oppdatering av behandling/fagsak. Påkrevd før lagring.
     * Convenience metode som tar hele entiteten.
     *
     * @see #taSkriveLås(Long, Long)
     */
    public BehandlingLås taSkriveLås(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling");
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
            "SELECT  b from Behandling b where b.fagsak.id=:fagsakId and behandlingType in (:behandlingType) order by b.opprettetTidspunkt desc",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setParameter("behandlingType", behandlingType);
        if (readOnly) {
            query.setHint(QueryHints.HINT_READONLY, "true");
        }
        return optionalFirst(medAktiveBehandlingTilstanderFilter(query::getResultList));
    }

    private Optional<Behandling> finnSisteBehandling(Long fagsakId, boolean readOnly) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT b from Behandling b where b.fagsak.id=:fagsakId order by b.opprettetTidspunkt desc",
            Behandling.class);
        query.setParameter(FAGSAK_ID, fagsakId);
        query.setMaxResults(1);
        if (readOnly) {
            query.setHint(QueryHints.HINT_READONLY, "true");
        }
        return optionalFirst(medAktiveBehandlingTilstanderFilter(query::getResultList));
    }

    private TypedQuery<Behandling> lagBehandlingQuery(Long behandlingId) {
        Objects.requireNonNull(behandlingId, BEHANDLING_ID); // NOSONAR

        TypedQuery<Behandling> query = getEntityManager().createQuery("from Behandling where id=:" + BEHANDLING_ID, Behandling.class);
        query.setParameter(BEHANDLING_ID, behandlingId);
        return query;
    }

    private TypedQuery<Behandling> lagBehandlingQuery(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, BEHANDLING_UUID); // NOSONAR

        TypedQuery<Behandling> query = getEntityManager().createQuery("from Behandling where uuid=:" + BEHANDLING_UUID, Behandling.class);
        query.setParameter(BEHANDLING_UUID, behandlingUuid);
        return query;
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
        getEntityManager().flush();

        return behandling.getId();
    }

    public Boolean erVersjonUendret(Long behandlingId, Long versjon) {
        Query query = getEntityManager().createNativeQuery(
            "SELECT 1 FROM behandling WHERE behandling.id = ? AND behandling.versjon = ?");
        query.setParameter(1, behandlingId);
        query.setParameter(2, versjon);
        return !query.getResultList().isEmpty();
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

    public long antallTilbakeføringerSiden(Long behandlingId, LocalDateTime tidspunkt) {
        Query query = getEntityManager().createNativeQuery(
            "SELECT count(*) FROM behandling_steg_tilstand WHERE behandling_id = :behandling_id AND behandling_steg_status = :tilbakeført_status AND opprettet_tid > :tidspunkt");
        query.setParameter("behandling_id", behandlingId);
        query.setParameter("tilbakeført_status", BehandlingStegStatus.TILBAKEFØRT.getKode());
        query.setParameter("tidspunkt", tidspunkt);

        return (Long) query.getSingleResult();
    }


    private <T> T medAktiveBehandlingTilstanderFilter(Supplier<T> aksjon) {
        EntityManager unwrappedEntityManager = unwrapEntityManager(entityManager);
        Session session = unwrappedEntityManager.unwrap(Session.class);
        String filternavn = "kunAktiveBehandlingTilstander";
        if (session.getEnabledFilter(filternavn) != null) {
            throw new IllegalArgumentException("nøstet bruk av metoden");
        }
        session.enableFilter(filternavn);
        T resultat = aksjon.get();
        session.disableFilter(filternavn);

        return resultat;

    }

    private static EntityManager unwrapEntityManager(EntityManager bean) {
        if (bean instanceof TargetInstanceProxy<?> tip) {
            return (EntityManager) tip.weld_getTargetInstance();
        } else {
            return bean;
        }
    }
}
