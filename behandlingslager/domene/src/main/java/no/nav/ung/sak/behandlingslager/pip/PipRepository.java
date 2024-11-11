package no.nav.ung.sak.behandlingslager.pip;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

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

        String sql = "SELECT " +
            "b.behandling_status behandligStatus, " +
            "b.ansvarlig_saksbehandler ansvarligSaksbehandler, " +
            "f.id fagsakId, " +
            "f.fagsak_status fagsakStatus, " +
            "f.saksnummer " +
            "FROM BEHANDLING b " +
            "JOIN FAGSAK f ON b.fagsak_id = f.id " +
            "WHERE b.id = :behandlingId";

        Query query = entityManager.createNativeQuery(sql, "PipDataResult");
        query.setParameter("behandlingId", behandlingId);

        @SuppressWarnings("rawtypes")
        List resultater = query.getResultList();
        if (resultater.isEmpty()) {
            return Optional.empty();
        } else if (resultater.size() == 1) {
            return Optional.of((PipBehandlingsData) resultater.get(0));
        } else {
            throw new IllegalStateException(
                "Forventet 0 eller 1 treff etter søk på behandlingId, fikk flere for behandlingId " + behandlingId);
        }
    }

    public Optional<PipBehandlingsData> hentDataForBehandlingUuid(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid"); // NOSONAR

        String sql = "SELECT " +
            "b.behandling_status behandligStatus, " +
            "b.ansvarlig_saksbehandler ansvarligSaksbehandler, " +
            "f.id fagsakId, " +
            "f.fagsak_status fagsakStatus, " +
            "f.saksnummer " +
            "FROM BEHANDLING b " +
            "JOIN FAGSAK f ON b.fagsak_id = f.id " +
            "WHERE b.uuid = :behandlingUuid";

        Query query = entityManager.createNativeQuery(sql, "PipDataResult");
        query.setParameter("behandlingUuid", behandlingUuid);

        @SuppressWarnings("rawtypes")
        List resultater = query.getResultList();
        if (resultater.isEmpty()) {
            return Optional.empty();
        } else if (resultater.size() == 1) {
            return Optional.of((PipBehandlingsData) resultater.get(0));
        } else {
            throw new IllegalStateException(
                "Forventet 0 eller 1 treff etter søk på behandlingId, fikk flere for behandlingUuid "
                    + behandlingUuid);
        }
    }

    public Set<AktørId> hentAktørIdKnyttetTilFagsaker(Collection<Long> fagsakIder) {
        Objects.requireNonNull(fagsakIder, SAKSNUMMER);
        if (fagsakIder.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = """
            SELECT por.AKTOER_ID From Fagsak fag
             INNER JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
             INNER JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
             INNER JOIN PO_INFORMASJON poi ON grp.registrert_informasjon_id = poi.ID
             INNER JOIN PO_PERSONOPPLYSNING por ON poi.ID = por.po_informasjon_id
             WHERE fag.id in (:fagsakIder) AND grp.aktiv = TRUE
             UNION ALL
             SELECT fag.bruker_aktoer_id FROM Fagsak fag
             WHERE fag.id in (:fagsakIder) AND fag.bruker_aktoer_id IS NOT NULL
             UNION ALL
             SELECT fag.pleietrengende_aktoer_id FROM Fagsak fag
             WHERE fag.id in (:fagsakIder) AND fag.pleietrengende_aktoer_id IS NOT NULL
             UNION ALL
             SELECT fag.relatert_person_aktoer_id FROM Fagsak fag
             WHERE fag.id in (:fagsakIder) AND fag.relatert_person_aktoer_id IS NOT NULL
             UNION ALL
             SELECT soa.aktoer_id from so_soeknad_angitt_person soa
             INNER JOIN so_soeknad so on so.id=soa.soeknad_id
             INNER JOIN gr_soeknad gr on gr.soeknad_id=so.id
             INNER JOIN behandling b on b.id=gr.behandling_id
             INNER JOIN fagsak fag on fag.id=b.fagsak_id
             WHERE fag.id in (:fagsakIder) and soa.aktoer_id is not null
             UNION ALL
             SELECT fag2.bruker_aktoer_id
             FROM Fagsak fag INNER JOIN Fagsak fag2 ON (
              fag.pleietrengende_aktoer_id IS NOT NULL AND fag.pleietrengende_aktoer_id = fag2.pleietrengende_aktoer_id
              OR fag.relatert_person_aktoer_id IS NOT NULL AND fag.relatert_person_aktoer_id = fag2.relatert_person_aktoer_id
             )
             WHERE fag.id in (:fagsakIder)
             """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fagsakIder", fagsakIder);

        @SuppressWarnings("unchecked")
        List<String> aktørIdList = query.getResultList();
        return aktørIdList.stream().map(AktørId::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<AktørId> hentAktørIdKnyttetTilSaksnummer(Saksnummer saksnummer) {
        Objects.requireNonNull(saksnummer, SAKSNUMMER);

        String sql = """
            SELECT por.AKTOER_ID From Fagsak fag
             INNER JOIN BEHANDLING beh ON fag.ID = beh.FAGSAK_ID
             INNER JOIN GR_PERSONOPPLYSNING grp ON grp.behandling_id = beh.ID
             INNER JOIN PO_INFORMASJON poi ON grp.registrert_informasjon_id = poi.ID
             INNER JOIN PO_PERSONOPPLYSNING por ON poi.ID = por.po_informasjon_id
             WHERE fag.SAKSNUMMER = (:saksnummer) AND grp.aktiv = TRUE
             UNION ALL
             SELECT fag.bruker_aktoer_id FROM Fagsak fag
             WHERE fag.SAKSNUMMER = (:saksnummer) AND fag.bruker_aktoer_id IS NOT NULL
             UNION ALL
             SELECT fag.pleietrengende_aktoer_id FROM Fagsak fag
             WHERE fag.SAKSNUMMER = (:saksnummer) AND fag.pleietrengende_aktoer_id IS NOT NULL
             UNION ALL
             SELECT fag.relatert_person_aktoer_id FROM Fagsak fag
             WHERE fag.SAKSNUMMER = (:saksnummer) AND fag.relatert_person_aktoer_id IS NOT NULL
             UNION ALL
             SELECT soa.aktoer_id from so_soeknad_angitt_person soa
             INNER JOIN so_soeknad so on so.id=soa.soeknad_id
             INNER JOIN gr_soeknad gr on gr.soeknad_id=so.id
             INNER JOIN behandling b on b.id=gr.behandling_id
             INNER JOIN fagsak fag on fag.id=b.fagsak_id
             WHERE fag.SAKSNUMMER = (:saksnummer) and soa.aktoer_id is not null
             UNION ALL
             SELECT fag2.bruker_aktoer_id
             FROM Fagsak fag INNER JOIN Fagsak fag2 ON (
              fag.pleietrengende_aktoer_id IS NOT NULL AND fag.pleietrengende_aktoer_id = fag2.pleietrengende_aktoer_id
              OR fag.relatert_person_aktoer_id IS NOT NULL AND fag.relatert_person_aktoer_id = fag2.relatert_person_aktoer_id
             )
             WHERE fag.SAKSNUMMER = (:saksnummer);
            """;

        Query query = entityManager.createNativeQuery(sql); // NOSONAR
        query.setParameter(SAKSNUMMER, saksnummer.getVerdi());

        @SuppressWarnings("unchecked")
        List<String> aktørIdList = query.getResultList();
        return aktørIdList.stream().map(AktørId::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    @SuppressWarnings({"unchecked"})
    public Set<Long> fagsakIdForJournalpostId(Collection<JournalpostId> journalpostId) {
        if (journalpostId.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT fagsak_id FROM JOURNALPOST WHERE journalpost_id in (:journalpostId)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("journalpostId", journalpostId.stream().map(j -> j.getVerdi()).collect(Collectors.toList()));

        var result = (List<Number>) query.getResultList();
        return result.stream().map(Number::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
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
    public Set<Long> fagsakIderForSøker(Collection<AktørId> aktørId) {
        if (aktørId.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT f.id " +
            "from FAGSAK f " +
            "where f.bruker_aktoer_id in (:aktørId)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("aktørId", aktørId.stream().map(AktørId::getId).collect(Collectors.toList()));
        var result = (List<Number>) query.getResultList();
        return result.stream().map(Number::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings({"unchecked"})
    public Set<Long> fagsakIdForSaksnummer(Collection<Saksnummer> saksnummere) {
        if (saksnummere.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT id from FAGSAK where saksnummer in (:saksnummre) ";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("saksnummre", saksnummere.stream().map(Saksnummer::getVerdi).collect(Collectors.toSet()));
        var result = (List<Number>) query.getResultList();
        return result.stream().map(Number::longValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Long> behandlingsIdForUuid(Set<UUID> behandlingsUUIDer) {
        if (behandlingsUUIDer == null || behandlingsUUIDer.isEmpty()) {
            return Collections.emptySet();
        }
        String sql = "SELECT beh.id from Behandling AS beh WHERE beh.uuid IN (:uuider)";
        TypedQuery<Long> query = entityManager.createQuery(sql, Long.class);
        query.setParameter("uuider", behandlingsUUIDer);
        return query.getResultStream().collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
