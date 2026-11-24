package no.nav.ung.sak.behandlingslager.behandling.kontroll;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;

/**
 * Fersk lesing av behandlinger på en fagsak for porten og synken mellom parallelle behandlinger.
 * <p>
 * Spørringene går direkte mot databasen og returnerer verdier, ikke managed entiteter. Hibernate-sesjonen kan ellers gi
 * gammel tilstand for behandlinger som er lastet tidligere i samme transaksjon. Kallestedet må holde fagsaklåsen
 * ({@code FagsakLåsRepository.taLås}) for at svaret skal være stabilt til transaksjonen er ferdig.
 */
@Dependent
public class ParallelleBehandlingerRepository {

    private static final String FAGSAK_ID = "fagsakId";

    private EntityManager entityManager;

    ParallelleBehandlingerRepository() {
        // for CDI proxy
    }

    @Inject
    public ParallelleBehandlingerRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Long> hentOriginalBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        @SuppressWarnings("unchecked")
        List<Number> resultat = entityManager.createNativeQuery(
                "select b.original_behandling_id from behandling b where b.id = :behandlingId")
            .setParameter("behandlingId", behandlingId)
            .getResultList();
        if (resultat.isEmpty()) {
            throw new IllegalArgumentException("Fant ikke behandling " + behandlingId);
        }
        return Optional.ofNullable(resultat.getFirst()).map(Number::longValue);
    }

    public BehandlingStatus hentStatus(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        @SuppressWarnings("unchecked")
        List<String> resultat = entityManager.createNativeQuery(
                "select b.behandling_status from behandling b where b.id = :behandlingId")
            .setParameter("behandlingId", behandlingId)
            .getResultList();
        if (resultat.isEmpty()) {
            throw new IllegalArgumentException("Fant ikke behandling " + behandlingId);
        }
        return BehandlingStatus.fraKode(resultat.getFirst());
    }

    /**
     * Siste ytelsesbehandling med status {@link BehandlingStatus#AVSLUTTET} som ikke er henlagt, sortert på vedtakstidspunkt
     * som {@code BehandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling}.
     * <p>
     * Behandlinger under iverksetting tas ikke med. De er ikke ferdige, og synken kjøres først når de avsluttes.
     */
    public Optional<Long> finnSisteAvsluttedeIkkeHenlagteYtelsesbehandlingId(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        @SuppressWarnings("unchecked")
        List<Tuple> resultat = entityManager.createNativeQuery("""
                select b.id as behandlingId, b.behandling_resultat_type as resultatType
                from behandling b
                join behandling_vedtak v on v.behandling_id = b.id
                where b.fagsak_id = :fagsakId
                  and b.behandling_status = :avsluttet
                  and b.behandling_type in (:typer)
                order by v.vedtak_dato desc, v.endret_tid desc
                """, Tuple.class)
            .setParameter(FAGSAK_ID, fagsakId)
            .setParameter("avsluttet", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("typer", koder(BehandlingType.getYtelseBehandlingTyper()))
            .getResultList();
        return resultat.stream()
            .filter(t -> !erHenlagt(BehandlingResultatType.fraKode(t.get("resultatType", String.class))))
            .map(t -> t.get("behandlingId", Number.class).longValue())
            .findFirst();
    }

    /**
     * Ytelsesbehandlinger på fagsaken som ikke har status {@link BehandlingStatus#AVSLUTTET}, også behandlinger under iverksetting.
     */
    public List<ParallellBehandling> hentIkkeAvsluttedeYtelsesbehandlinger(Long fagsakId) {
        Objects.requireNonNull(fagsakId, FAGSAK_ID);
        @SuppressWarnings("unchecked")
        List<Tuple> resultat = entityManager.createNativeQuery("""
                select b.id as behandlingId,
                       b.behandling_type as behandlingType,
                       b.behandling_status as status,
                       b.behandling_resultat_type as resultatType,
                       bst.behandling_steg as steg,
                       bst.behandling_steg_status as stegStatus
                from behandling b
                left join behandling_steg_tilstand bst on bst.behandling_id = b.id and bst.aktiv = true
                where b.fagsak_id = :fagsakId
                  and b.behandling_status <> :avsluttet
                  and b.behandling_type in (:typer)
                order by b.id
                """, Tuple.class)
            .setParameter(FAGSAK_ID, fagsakId)
            .setParameter("avsluttet", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("typer", koder(BehandlingType.getYtelseBehandlingTyper()))
            .getResultList();

        Map<Long, List<Tuple>> perBehandling = new LinkedHashMap<>();
        for (Tuple t : resultat) {
            perBehandling.computeIfAbsent(t.get("behandlingId", Number.class).longValue(), id -> new ArrayList<>()).add(t);
        }
        return perBehandling.entrySet().stream()
            .map(e -> tilParallellBehandling(e.getKey(), e.getValue()))
            .toList();
    }

    public List<ParallellBehandling> hentAndreIkkeAvsluttedeYtelsesbehandlinger(Long fagsakId, Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        return hentIkkeAvsluttedeYtelsesbehandlinger(fagsakId).stream()
            .filter(b -> !b.behandlingId().equals(behandlingId))
            .toList();
    }

    public Optional<ParallellBehandling> finnIkkeAvsluttetKontrollbehandling(Long fagsakId) {
        List<ParallellBehandling> kontrollbehandlinger = hentIkkeAvsluttedeYtelsesbehandlinger(fagsakId).stream()
            .filter(ParallellBehandling::erKontrollbehandling)
            .toList();
        if (kontrollbehandlinger.size() > 1) {
            throw new IllegalStateException("Fant " + kontrollbehandlinger.size() + " kontrollbehandlinger som ikke er avsluttet på fagsak " + fagsakId);
        }
        return kontrollbehandlinger.stream().findFirst();
    }

    private static ParallellBehandling tilParallellBehandling(Long behandlingId, List<Tuple> rader) {
        Set<BehandlingStegType> aktiveSteg = rader.stream()
            .map(ParallelleBehandlingerRepository::utledAktivtSteg)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (aktiveSteg.size() > 1) {
            throw new IllegalStateException("Behandling " + behandlingId + " har flere aktive steg: " + aktiveSteg);
        }
        Tuple første = rader.getFirst();
        return new ParallellBehandling(
            behandlingId,
            BehandlingType.fraKode(første.get("behandlingType", String.class)),
            BehandlingStatus.fraKode(første.get("status", String.class)),
            BehandlingResultatType.fraKode(første.get("resultatType", String.class)),
            aktiveSteg.stream().findFirst().orElse(null));
    }

    private static BehandlingStegType utledAktivtSteg(Tuple t) {
        String steg = t.get("steg", String.class);
        if (steg == null || BehandlingStegStatus.erSluttStatus(BehandlingStegStatus.fraKode(t.get("stegStatus", String.class)))) {
            return null;
        }
        return BehandlingStegType.fraKode(steg);
    }

    private static boolean erHenlagt(BehandlingResultatType resultatType) {
        return resultatType != null && resultatType.erHenleggelse();
    }

    private static List<String> koder(Set<BehandlingType> typer) {
        return typer.stream().map(BehandlingType::getKode).toList();
    }
}
