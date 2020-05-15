package no.nav.k9.sak.metrikker;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import org.hibernate.query.NativeQuery;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;

@Dependent
public class StatistikkRepository {
    private EntityManager entityManager;

    @Inject
    public StatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> avslagStatistikk() {

        String sql = "select f.ytelse_type, f.fagsak_status, b.behandling_type, b.behandling_resultat_type, vv.vilkar_type, coalesce(vrp.avslag_kode, '-') avslag_kode" +
            " , count(*) antall from fagsak f " +
            " inner join behandling b on b.fagsak_id=f.id" +
            " inner join rs_vilkars_resultat rs on rs.behandling_id=b.id and rs.aktiv=true" +
            " inner join VR_VILKAR_RESULTAT vr on vr.id=rs.vilkarene_id" +
            " inner join vr_vilkar vv on vv.vilkar_resultat_id=vr.id" +
            " inner join vr_vilkar_periode vrp on vrp.vilkar_id=vv.id" +
            " group by 1, 2, 3, 4, 5, 6";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        return stream.map(t -> SensuEvent.createSensuEvent("totalt_vilkar_resultat_v2",
            toMap(
                "ytelse_type", t.get(0, String.class),
                "fagsak_status", t.get(1, String.class),
                "behandling_type", t.get(2, String.class),
                "behandling_resultat_type", t.get(3, String.class),
                "vilkar_type", t.get(4, String.class),
                "avslag_kode", t.get(5, String.class)),
            Map.of("totalt_antall", t.get(6, BigInteger.class)))).collect(Collectors.toList());

    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> aksjonspunktStatistikk() {
        String sql = "select f.ytelse_type, a.aksjonspunkt_def as aksjonspunkt, a.aksjonspunkt_status," +
            " count(*) as antall" +
            " from aksjonspunkt a " +
            " inner join behandling b on b.id =a.behandling_id" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " group by 1, 2, 3";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        return stream.map(t -> SensuEvent.createSensuEvent("aksjonspunkt_per_ytelse_type_v2",
            toMap(
                "ytelse_type", t.get(0, String.class),
                "aksjonspunkt", t.get(1, String.class),
                "aksjonspunkt_status", t.get(2, String.class)),
            Map.of("totalt_antall", t.get(3, BigInteger.class)))).collect(Collectors.toList());

    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> aksjonspunktVenteårsakStatistikk() {

        String sql = "select f.ytelse_type, a.aksjonspunkt_def as aksjonspunkt, a.aksjonspunkt_status, a.vent_aarsak, v.vent_aarsak, " +
            "             case when a.vent_aarsak=v.vent_aarsak then count(*) else 0 end as antall" +
            "             from aksjonspunkt a" +
            "             cross join (select distinct vent_aarsak from aksjonspunkt where vent_aarsak!='-') v " +
            "                         inner join behandling b on b.id =a.behandling_id " +
            "             inner join fagsak f on f.id=b.fagsak_id" +
            "                         " +
            "             group by 1, 2, 3, 4, 5" +
            "                         order by 1, 2, 3, 4, 5";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        return stream.map(t -> SensuEvent.createSensuEvent("aksjonspunkt_ytelse_type_vent_aarsak_v2",
            toMap(
                "ytelse_type", t.get(0, String.class),
                "aksjonspunkt", t.get(1, String.class),
                "aksjonspunkt_status", t.get(2, String.class),
                "vent_aarsak", t.get(4, String.class)),
            Map.of("totalt_antall", t.get(5, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> behandlingStatistikkUnderBehandling() {
        String sql = "select f.ytelse_type, behandling_type, behandling_status" +
            " , count(*) antall" +
            " from behandling b" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " group by 1, 2, 3";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        return stream.map(t -> SensuEvent.createSensuEvent("behandling_status_v2",
            toMap(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", BehandlingType.fraKode(t.get(1, String.class)).getNavn(),
                "behandling_status", t.get(2, String.class)),
            Map.of("totalt_antall", t.get(3, BigInteger.class)))).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    List<SensuEvent> prosessTaskStatistikk() {
        String sql = " select ytelse_type, task_type, status, sum(antall) as antall from ("
            + " select t.kode as task_type, s.status, coalesce(f.ytelse_type, 'OBSOLETE') as ytelse_type, p.status as dummy, case when p.status is null then 0 else count(*) end as antall " +
            " from prosess_task_type t" +
            " cross join(values ('FEILET'),('VENTER_SVAR'),('KLAR')) as s(status)" +
            " left outer join prosess_task p on p.task_type=t.kode AND p.status=s.status and p.status in ('FEILET', 'VENTER_SVAR', 'KLAR')" +
            " left outer join fagsak_prosess_task fpt on fpt.prosess_task_id=p.id" +
            " left outer join fagsak f on f.id=fpt.fagsak_id" +
            " group by 1, 2, 3, 4 ) t" +
            " group by 1, 2, 3" +
            " order by 1, 2, 3";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        return stream.map(t -> SensuEvent.createSensuEvent("prosess_task",
            toMap(
                "ytelse_type", t.get(0, String.class),
                "prosess_task_type", t.get(1, String.class),
                "status", t.get(2, String.class)),
            Map.of("totalt_antall", t.get(4, BigInteger.class)))).collect(Collectors.toList());
    }

    /** Map.of() takler ikke null verdier, så vi lager vår egen variant. */
    private static Map<String, String> toMap(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Må ha partall antall argumenter, fikk: " + Arrays.asList(args));
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
     */
    public List<SensuEvent> hentAlle() {
        List<SensuEvent> metrikker = new ArrayList<>();
        metrikker.addAll(prosessTaskStatistikk());
        metrikker.addAll(behandlingStatistikkUnderBehandling());
        metrikker.addAll(aksjonspunktStatistikk());
        metrikker.addAll(aksjonspunktVenteårsakStatistikk());
        metrikker.addAll(avslagStatistikk());
        return metrikker;
    }
}
