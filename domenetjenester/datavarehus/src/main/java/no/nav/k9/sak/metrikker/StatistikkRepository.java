package no.nav.k9.sak.metrikker;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;

@Dependent
class StatistikkRepository {
    private EntityManager entityManager;

    @Inject
    StatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> aksjonspunktStatistikk() {
        String sql = "select a.aksjonspunkt_def as aksjonspunkt, f.ytelse_type, a.aksjonspunkt_status," +
            " case when (  a.aksjonspunkt_status != 'OPPR' OR f.ytelse_type='OBSOLETE' ) then 0 else count(*) end as antall" +
            " from aksjonspunkt a " +
            " inner join behandling b on b.id =a.behandling_id" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " group by 1, 2, 3";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("aksjonspunkt_per_ytelse_type",
            Map.of(
                "aksjonspunkt", t.get(0, String.class),
                "ytelse_type", t.get(1, String.class)),
            Map.of("totalt_antall", t.get(3, BigInteger.class)))).collect(Collectors.toList());

    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> aksjonspunktVenteårsakStatistikk() {
        
        String sql = "select a.aksjonspunkt_def as aksjonspunkt, f.ytelse_type, a.vent_aarsak, a.aksjonspunkt_status," +
                " case when (  a.aksjonspunkt_status != 'OPPR' OR f.ytelse_type='OBSOLETE' or vent_aarsak='-' or vent_aarsak is null ) then 0 else count(*) end as antall" +
                " from aksjonspunkt a " +
                " inner join behandling b on b.id =a.behandling_id" +
                " inner join fagsak f on f.id=b.fagsak_id" +
                " group by 1, 2, 3, 4";
        
        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("aksjonspunkt_ytelse_type_vent_aarsak",
            Map.of(
                "aksjonspunkt", t.get(0, String.class),
                "ytelse_type", t.get(1, String.class),
                "vent_aarsak", t.get(2, String.class)),
            Map.of("totalt_antall", t.get(4, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> behandlingStatistikkUnderBehandling() {
        String sql = "select f.ytelse_type, behandling_type, behandling_status," +
            " case when ( behandling_status='AVSLU' OR f.ytelse_type='OBSOLETE' ) then 0 else count(*) end as antall from behandling b" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " group by 1, 2, 3";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("behandling_status",
            Map.of(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", BehandlingType.fraKode(t.get(1, String.class)).getNavn(),
                "behandling_status", t.get(2, String.class)),
            Map.of("totalt_antall", t.get(3, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> behandlingStatistikkStartetIDag() {
        String sql = "select f.ytelse_type, behandling_type, to_char(opprettet_dato, 'YYYY-MM-DD'), count(*) as antall from behandling b" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " where f.ytelse_type!='OBSOLETE' and date_trunc('day', opprettet_dato) = CURRENT_DATE" +
            " group by 1, 2, 3";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("behandling_status_opprettet_dag",
            Map.of(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", BehandlingType.fraKode(t.get(1, String.class)).getNavn(),
                "behandling_status", t.get(2, String.class),
                "opprettet_dato", t.get(3, String.class)),
            Map.of("totalt_antall", t.get(4, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> behandlingStatistikkAvsluttetIDag() {
        String sql = "select f.ytelse_type, behandling_type, to_char(avsluttet_dato, 'YYYY-MM-DD'), count(*) as antall from behandling b" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " where behandling_status='AVSLU' and f.ytelse_type!='OBSOLETE' and date_trunc('day', avsluttet_dato) = CURRENT_DATE" +
            " group by 1, 2, 3";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("behandling_status_avsluttet_dag",
            Map.of(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", BehandlingType.fraKode(t.get(1, String.class)).getNavn(),
                "behandling_status", t.get(2, String.class),
                "avsluttet_dato", t.get(3, String.class)),
            Map.of("totalt_antall", t.get(4, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> prosessTaskStatistikk() {
        String sql = "select t.kode as task_type, p.status, coalesce(count(p.status), 0) as antall " +
            " from prosess_task_type t" +
            " left outer join prosess_task p on p.task_type=t.kode And  p.status in ('FEILET', 'SUSPENDERT', 'VENTER_SVAR')" +
            " group by 1, 2";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("prosess_task",
            Map.of(
                "prosess_task_type", t.get(0, String.class),
                "status", t.get(1, String.class)),
            Map.of("totalt_antall", t.get(2, BigInteger.class)))).collect(Collectors.toList());
    }
}
