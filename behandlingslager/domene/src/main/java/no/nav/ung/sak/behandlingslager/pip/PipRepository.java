package no.nav.ung.sak.behandlingslager.pip;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Dependent
public class PipRepository {

    public static final String SAKSNUMMER = "saksnummer";
    private final EntityManager entityManager;

    @Inject
    public PipRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<PipBehandlingsData> hentDataForBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR

        String sql = """
            SELECT
               b.uuid behandlingUuid,
               b.behandling_status behandlingStatus,
               b.ansvarlig_saksbehandler ansvarligSaksbehandler,
               f.id fagsakId,
               f.fagsak_status fagsakStatus,
               f.saksnummer
             FROM BEHANDLING b
             JOIN FAGSAK f ON b.fagsak_id = f.id
             WHERE b.id = :behandlingId""";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        query.setParameter("behandlingId", behandlingId);

        @SuppressWarnings("rawtypes")
        List<Tuple> resultater = query.getResultList();
        if (resultater.isEmpty()) {
            return Optional.empty();
        } else if (resultater.size() == 1) {
            return Optional.of(mapPipBehandlingsDataTuple(resultater.getFirst()));
        } else {
            throw new IllegalStateException(
                "Forventet 0 eller 1 treff etter søk på behandlingId, fikk flere for behandlingId " + behandlingId);
        }
    }

    private PipBehandlingsData mapPipBehandlingsDataTuple(Tuple t) {
        return new PipBehandlingsData(
            t.get("behandlingUuid", UUID.class),
            BehandlingStatus.fraKode( t.get("behandlingStatus", String.class)),
            FagsakStatus.fraKode(t.get("fagsakStatus", String.class)),
            t.get("ansvarligSaksbehandler", String.class),
            new Saksnummer(t.get("saksnummer", String.class))
        );
    }

    public Optional<PipBehandlingsData> hentDataForBehandlingUuid(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid"); // NOSONAR

        String sql = """
            SELECT
                b.uuid behandlingUuid,
                b.behandling_status behandlingStatus,
                b.ansvarlig_saksbehandler ansvarligSaksbehandler,
                f.id fagsakId,
                f.fagsak_status fagsakStatus,
                f.saksnummer
             FROM BEHANDLING b
             JOIN FAGSAK f ON b.fagsak_id = f.id
             WHERE b.uuid = :behandlingUuid""";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        query.setParameter("behandlingUuid", behandlingUuid);

        @SuppressWarnings("rawtypes")
        List resultater = query.getResultList();
        if (resultater.isEmpty()) {
            return Optional.empty();
        } else if (resultater.size() == 1) {
            return Optional.of(mapPipBehandlingsDataTuple((Tuple) resultater.getFirst()));
        } else {
            throw new IllegalStateException(
                "Forventet 0 eller 1 treff etter søk på behandlingId, fikk flere for behandlingUuid "
                    + behandlingUuid);
        }
    }

    public Set<AktørId> hentAktørIdKnyttetTilFagsaker(Collection<Saksnummer> saksnumre) {
        Objects.requireNonNull(saksnumre, SAKSNUMMER);
        if (saksnumre.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = """
            SELECT por.AKTOER_ID
             From Fagsak fag
             INNER JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
             INNER JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
             INNER JOIN PO_INFORMASJON poi ON grp.registrert_informasjon_id = poi.ID
             INNER JOIN PO_PERSONOPPLYSNING por ON poi.ID = por.po_informasjon_id
             WHERE fag.saksnummer in (:saksnumre) AND grp.aktiv = TRUE
             UNION ALL
             SELECT fag.bruker_aktoer_id
             FROM Fagsak fag
             WHERE fag.saksnummer in (:saksnumre) AND fag.bruker_aktoer_id IS NOT NULL
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("saksnumre", saksnumre.stream().map(Saksnummer::getVerdi).collect(Collectors.toSet()));

        @SuppressWarnings("unchecked")
        List<String> aktørIdList = query.getResultList();
        return aktørIdList.stream().map(AktørId::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings({"unchecked"})
    public Set<Saksnummer> saksnumreForJournalpostId(Collection<JournalpostId> journalpostId) {
        if (journalpostId.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT saksnummer FROM JOURNALPOST jp JOIN FAGSAK f on f.id = jp.fagsak_id WHERE journalpost_id in (:journalpostId)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("journalpostId", journalpostId.stream().map(j -> j.getVerdi()).collect(Collectors.toList()));

        var result = (List<String>) query.getResultList();
        return result.stream().map(Saksnummer::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings({"unchecked"})
    public Set<Long> behandlingsIdForOppgaveId(Collection<String> oppgaveIder) {
        if (oppgaveIder.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT behandling_id FROM OPPGAVE_BEHANDLING_KOBLING WHERE oppgave_id in (:oppgaveIder)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("oppgaveIder", oppgaveIder);
        var result = (List<Number>) query.getResultList();
        return result.stream().map(Number::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> hentAksjonspunktTypeForAksjonspunktKoder(Collection<String> aksjonspunktKoder) {
        if (aksjonspunktKoder.isEmpty()) {
            return Collections.emptySet();
        }
        return aksjonspunktKoder.stream()
            .map(ak -> AksjonspunktDefinisjon.fraKode(ak).getAksjonspunktType().getOffisiellKode())
            .sorted()
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings({"unchecked"})
    public Set<Saksnummer> saksnumreForSøker(Collection<AktørId> aktørId) {
        if (aktørId.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT f.saksnummer " +
            "from FAGSAK f " +
            "where f.bruker_aktoer_id in (:aktørId)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("aktørId", aktørId.stream().map(AktørId::getId).collect(Collectors.toList()));
        var result = (List<String>) query.getResultList();
        return result.stream().map(Saksnummer::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Saksnummer> finnSaksnumerSomEksisterer(Collection<Saksnummer> saksnumre) {
        if (saksnumre.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT saksnummer from FAGSAK where saksnummer in (:saksnumre) ";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("saksnumre", saksnumre.stream().map(Saksnummer::getVerdi).toList());
        var result = (List<String>) query.getResultList();
        return result.stream().map(Saksnummer::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Long> behandlingsIdForUuid(Set<UUID> behandlingsUUIDer) {
        if (behandlingsUUIDer.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT beh.id from Behandling AS beh WHERE beh.uuid IN (:uuider)";
        TypedQuery<Long> query = entityManager.createQuery(sql, Long.class);
        query.setParameter("uuider", behandlingsUUIDer);
        return query.getResultStream().collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
