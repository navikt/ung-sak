package no.nav.k9.sak.metrikker;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class StatistikkRepository {
    private EntityManager entityManager;

    @Inject
    public StatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> avslagStatistikk(LocalDate fagsakOpprettetDato) {

        String sql = "select f.ytelse_type, f.fagsak_status, b.behandling_type, b.behandling_resultat_type, b.uuid, vv.vilkar_type, coalesce(vrp.avslag_kode, '-'), f.opprettet_tid from fagsak f " +
            "inner join behandling b on b.fagsak_id=f.id" +
            "inner join rs_vilkars_resultat rs on rs.behandling_id=b.id and rs.aktiv=true" +
            "inner join VR_VILKAR_RESULTAT vr on vr.id=rs.vilkarene_id" +
            "inner join vr_vilkar vv on vv.vilkar_resultat_id=vr.id" +
            "inner join vr_vilkar_periode vrp on vrp.vilkar_id=vv.id" +
            "where date_trunc('DAY', f.opprettet_tid) = coalesce(:dato, date_trunc('DAY', f.opprettet_tid))";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("dato", fagsakOpprettetDato);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("totalt_vilkar_resultat",
            toMap(
                "ytelse_type", t.get(0, String.class),
                "fagsak_status", t.get(1, String.class),
                "behandling_type", t.get(2, String.class),
                "behandling_resultat_type", t.get(3, String.class),
                "behandling_uuid", t.get(4, String.class),
                "vilkar_type", t.get(5, String.class),
                "avslag_kode", t.get(6, String.class)),
            Map.of("antall", "1"), tidsstempel(fagsakOpprettetDato, t.get(7, LocalDateTime.class)))).collect(Collectors.toList());

    }

    private long tidsstempel(LocalDate fagsakOpprettetDato, LocalDateTime opprettetTid) {
        return fagsakOpprettetDato == null ? System.currentTimeMillis() : opprettetTid.toInstant(ZoneOffset.of("Z")).toEpochMilli();
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> aksjonspunktStatistikk(LocalDate fagsakOpprettetDato) {
        String sql = "select a.aksjonspunkt_def as aksjonspunkt, f.ytelse_type, a.aksjonspunkt_status," +
            " case when (  a.aksjonspunkt_status != 'OPPR' OR f.ytelse_type='OBSOLETE' ) then 0 else count(*) end as antall" +
            " from aksjonspunkt a " +
            " inner join behandling b on b.id =a.behandling_id" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " where date_trunc('DAY', f.opprettet_tid) = coalesce(:dato, date_trunc('DAY', f.opprettet_tid)) " +
            " group by 1, 2, 3";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("dato", fagsakOpprettetDato);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("aksjonspunkt_per_ytelse_type",
            toMap(
                "aksjonspunkt", t.get(0, String.class),
                "ytelse_type", t.get(1, String.class)),
            Map.of("totalt_antall", t.get(3, BigInteger.class)))).collect(Collectors.toList());

    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> aksjonspunktVenteårsakStatistikk(LocalDate fagsakOpprettetDato) {

        String sql = "select a.aksjonspunkt_def as aksjonspunkt, f.ytelse_type, a.aksjonspunkt_status, a.vent_aarsak, " +
            " case when (  a.aksjonspunkt_status != 'OPPR' OR f.ytelse_type='OBSOLETE' or vent_aarsak='-' or vent_aarsak is null ) then 0 else count(*) end as antall" +
            " from aksjonspunkt a " +
            " inner join behandling b on b.id =a.behandling_id" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " where date_trunc('DAY', f.opprettet_tid) = coalesce(:dato, date_trunc('DAY', f.opprettet_tid)) " +
            " group by 1, 2, 3, 4";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("dato", fagsakOpprettetDato);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("aksjonspunkt_ytelse_type_vent_aarsak",
            toMap(
                "aksjonspunkt", t.get(0, String.class),
                "ytelse_type", t.get(1, String.class),
                "vent_aarsak", t.get(3, String.class)),
            Map.of("totalt_antall", t.get(4, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> behandlingStatistikkUnderBehandling(LocalDate fagsakOpprettetDato) {
        String sql = "select f.ytelse_type, behandling_type, behandling_status," +
            " case when ( behandling_status='AVSLU' OR f.ytelse_type='OBSOLETE' ) then 0 else count(*) end as antall from behandling b" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " where date_trunc('DAY', f.opprettet_tid) = coalesce(:dato, date_trunc('DAY', f.opprettet_tid)) " +
            " group by 1, 2, 3";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("dato", fagsakOpprettetDato);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("behandling_status",
            toMap(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", BehandlingType.fraKode(t.get(1, String.class)).getNavn(),
                "behandling_status", t.get(2, String.class)),
            Map.of("totalt_antall", t.get(3, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> prosessTaskStatistikk() {
        String sql = "select t.kode as task_type, s.status, coalesce(f.ytelse_type, '-'), p.status as dummy, case when p.status is null then 0 else count(p.status) end as antall " +
            " from prosess_task_type t" +
            " cross join(values ('FEILET'),('VENTER_SVAR'),('KLAR')) as s(status)" +
            " left outer join prosess_task p on p.task_type=t.kode And  p.status=s.status and p.status in ('FEILET', 'VENTER_SVAR', 'KLAR')" +
            " left outer join fagsak_prosess_task fpt on fpt.prosess_task_id=p.id" +
            " left outer join fagsak f on f.id=fpt.fagsak_id" +
            " group by 1, 2, 3, 4" +
            " order by dummy nulls last";

        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> SensuEvent.createSensuEvent("prosess_task",
            toMap(
                "prosess_task_type", t.get(0, String.class),
                "status", t.get(1, String.class),
                "ytelse_type", t.get(2, String.class)),
            Map.of("totalt_antall", t.get(3, BigInteger.class)))).collect(Collectors.toList());
    }

    /** Map.of() takler ikke null verdier, så vi lager vår egen variant. */
    private static Map<String, String> toMap(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Må ha partall antal argumenter, fikk: " + Arrays.asList(args));
        }
        var map = new HashMap<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            // influxdb Point takler ikke null key eller value. skipper null verdier
            String v = args[i + 1];
            if (v != null) {
                map.put(args[i], v);
            }
        }
        return map;
    }

    /**
     * @param fagsakOpprettetDato - optional dato, begrens søket til kun fagsaker opprettet denne datoen
     */
    public List<SensuEvent> hentAlle(LocalDate fagsakOpprettetDato) {
        List<SensuEvent> metrikker = new ArrayList<>();
        metrikker.addAll(prosessTaskStatistikk());
        metrikker.addAll(behandlingStatistikkUnderBehandling(fagsakOpprettetDato));
        metrikker.addAll(aksjonspunktStatistikk(fagsakOpprettetDato));
        metrikker.addAll(aksjonspunktVenteårsakStatistikk(fagsakOpprettetDato));
        metrikker.addAll(avslagStatistikk(fagsakOpprettetDato));
        return metrikker;
    }
}
