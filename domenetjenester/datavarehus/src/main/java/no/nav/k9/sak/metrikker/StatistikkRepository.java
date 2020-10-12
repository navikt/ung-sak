package no.nav.k9.sak.metrikker;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import org.hibernate.query.NativeQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskFeil;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

@Dependent
public class StatistikkRepository {

    private static final String UDEFINERT = "-";

    static final List<String> YTELSER = List.of(
        FagsakYtelseType.FRISINN,
        FagsakYtelseType.OMSORGSPENGER,
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
        .stream().map(k -> k.getKode()).collect(Collectors.toList());

    static final List<String> PROSESS_TASK_STATUSER = List.of(ProsessTaskStatus.KLAR, ProsessTaskStatus.FEILET, ProsessTaskStatus.VENTER_SVAR)
        .stream().map(k -> k.getDbKode()).collect(Collectors.toList());
    static final List<String> AKSJONSPUNKTER = AksjonspunktDefinisjon.kodeMap().values().stream()
        .filter(p -> !AksjonspunktDefinisjon.UNDEFINED.equals(p)).map(k -> k.getKode()).collect(Collectors.toList());
    static final List<String> AKSJONSPUNKT_STATUSER = AksjonspunktStatus.kodeMap().values().stream()
        .filter(p -> !AksjonspunktStatus.AVBRUTT.equals(p)).map(k -> k.getKode())
        .collect(Collectors.toList());

    static final List<String> BEHANDLING_RESULTAT_TYPER = List.copyOf(BehandlingResultatType.kodeMap().keySet());
    static final List<String> BEHANDLING_STATUS = List.copyOf(BehandlingStatus.kodeMap().keySet());
    static final List<String> FAGSAK_STATUS = List.copyOf(FagsakStatus.kodeMap().keySet());
    static final List<String> BEHANDLING_TYPER = BehandlingType.kodeMap().values().stream().filter(p -> !BehandlingType.UDEFINERT.equals(p)).map(k -> k.getKode()).collect(Collectors.toList());
    static final List<String> VILKÅRTYPER = VilkårType.kodeMap().values().stream().filter(p -> !VilkårType.UDEFINERT.equals(p)).map(k -> k.getKode()).collect(Collectors.toList());
    static final List<String> AVSLAGSÅRSAKER = Avslagsårsak.kodeMap().values().stream().filter(p -> !Avslagsårsak.UDEFINERT.equals(p)).map(k -> k.getKode()).collect(Collectors.toList());
    static final List<String> VENT_ÅRSAKER = Venteårsak.kodeMap().values().stream().filter(p -> !Venteårsak.UDEFINERT.equals(p)).map(k -> k.getKode()).collect(Collectors.toList());
    static final List<String> BREVKODER = Brevkode.registrerteKoder().values().stream().filter(p -> !Brevkode.UDEFINERT.equals(p)).map(k -> k.getKode()).collect(Collectors.toList());

    private static final ObjectMapper OM = new ObjectMapper();

    static final String PROSESS_TASK_VER = "v4";

    private EntityManager entityManager;

    @Inject
    public StatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<SensuEvent> hentAlle() {
        LocalDate dag = LocalDate.now();

        List<SensuEvent> metrikker = new ArrayList<>();
        metrikker.addAll(fagsakStatusStatistikk());
        metrikker.addAll(behandlingStatusStatistikk());
        metrikker.addAll(behandlingResultatStatistikk());
        metrikker.addAll(prosessTaskStatistikk());
        metrikker.addAll(mottattDokumentStatistikk());
        metrikker.addAll(aksjonspunktStatistikk());
        metrikker.addAll(aksjonspunktStatistikkDaglig(dag));
        metrikker.addAll(aksjonspunktVenteårsakStatistikk());
        metrikker.addAll(avslagStatistikk());
        metrikker.addAll(avslagStatistikkDaglig(dag));
        metrikker.addAll(prosessTaskFeilStatistikk());
        return metrikker;
    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> fagsakStatusStatistikk() {

        String sql = "select f.ytelse_type, f.fagsak_status, count(*) as antall" +
            "         from fagsak f" +
            "         group by 1, 2" +
            "         order by 1, 2";

        String metricField = "totalt_antall";
        String metricName = "fagsak_status_v2";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "fagsak_status", t.get(1, String.class)),
            Map.of("totalt_antall", t.get(2, BigInteger.class)))).collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden fagsak endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "fagsak_status", FAGSAK_STATUS),
            Map.of(
                metricField, BigInteger.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> behandlingResultatStatistikk() {

        String sql = "select f.ytelse_type, b.behandling_type, b.behandling_resultat_type,  " +
            "         count(*) antall, " +
            "         count(ansvarlig_saksbehandler) filter (where ansvarlig_saksbehandler is not null) manuell_antall, " +
            "         count(totrinnsbehandling) filter (where totrinnsbehandling=true) as totrinn " +
            "         from fagsak f  " +
            "         inner join behandling b on b.fagsak_id=f.id " +
            "         where b.behandling_status IN (:statuser) and behandling_resultat_type is not null " +
            "         group by 1, 2, 3 " +
            "         order by 1, 2, 3 ";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("statuser", Set.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode()));

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        String metricName = "behandling_resultat_v1";
        String metricField1 = "totalt_antall";
        String metricField2 = "totalt_antall_manuell";
        String metricField3 = "totalt_antall_totrinn";

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", t.get(1, String.class),
                "behandling_resultat_type", t.get(2, String.class)),
            Map.of(
                metricField1, t.get(3, BigInteger.class),
                metricField2, t.get(4, BigInteger.class),
                metricField3, t.get(5, BigInteger.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden aksjonspunkt endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "behandling_type", BEHANDLING_TYPER,
                "behandling_resultat_type", BEHANDLING_RESULTAT_TYPER),
            Map.of(
                metricField1, BigInteger.ZERO,
                metricField2, BigInteger.ZERO,
                metricField3, BigInteger.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> behandlingStatusStatistikk() {
        String sql = "select f.ytelse_type, b.behandling_type, b.behandling_status, count(*) as antall" +
            "      from fagsak f" +
            "      inner join behandling b on b.fagsak_id=f.id" +
            "      group by 1, 2, 3" +
            "      order by 1, 2;";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        String metricName = "behandling_status_v2";
        String metricField = "totalt_antall";

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", t.get(1, String.class),
                "behandling_status", t.get(2, String.class)),
            Map.of(
                metricField, t.get(3, BigInteger.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden behandling endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "behandling_type", BEHANDLING_TYPER,
                "behandling_status", BEHANDLING_STATUS),
            Map.of(
                metricField, BigInteger.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    /** trenger ikke sette 0 verdier for kombinasjoner som ikke fins, da dette ser kun på slutt resultater (vedtatte behandlinger). */
    @SuppressWarnings("unchecked")
    Collection<SensuEvent> avslagStatistikk() {

        String sql = "select f.ytelse_type, b.behandling_type, b.behandling_resultat_type, vv.vilkar_type, vrp.avslag_kode" +
            " , count(*) antall from fagsak f " +
            " inner join behandling b on b.fagsak_id=f.id" +
            " inner join rs_vilkars_resultat rs on rs.behandling_id=b.id and rs.aktiv=true" +
            " inner join VR_VILKAR_RESULTAT vr on vr.id=rs.vilkarene_id" +
            " inner join vr_vilkar vv on vv.vilkar_resultat_id=vr.id" +
            " inner join vr_vilkar_periode vrp on vrp.vilkar_id=vv.id" +
            " where b.behandling_status IN (:behStatuser) and vrp.avslag_kode != '-' and vrp.avslag_kode IS NOT NULL" +
            " group by 1, 2, 3, 4, 5";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behStatuser", Set.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode())); // kun ta med behandlinger som avsluttes (iverksettes, avsluttet)
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var metricName = "totalt_vilkar_resultat_v3";
        var metricField = "totalt_antall";
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "behandling_type", t.get(1, String.class),
                "behandling_resultat_type", t.get(2, String.class),
                "vilkar_type", t.get(3, String.class),
                "avslag_kode", t.get(4, String.class)),
            Map.of(
                metricField, t.get(5, BigInteger.class))))
            .collect(Collectors.toList());

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> avslagStatistikkDaglig(LocalDate dato) {
        String sql = "select f.ytelse_type, f.saksnummer, b.id as behandling_id, b.behandling_type, b.behandling_resultat_type, vv.vilkar_type, vrp.avslag_kode" +
            "    , coalesce(b.endret_tid, b.opprettet_tid) as tid " +
            " from fagsak f " +
            " inner join behandling b on b.fagsak_id=f.id" +
            " inner join rs_vilkars_resultat rs on rs.behandling_id=b.id and rs.aktiv=true" +
            " inner join VR_VILKAR_RESULTAT vr on vr.id=rs.vilkarene_id" +
            " inner join vr_vilkar vv on vv.vilkar_resultat_id=vr.id" +
            " inner join vr_vilkar_periode vrp on vrp.vilkar_id=vv.id" +
            " where b.behandling_status IN (:behStatuser) and vrp.avslag_kode != '-' and vrp.avslag_kode IS NOT NULL" +
            " and coalesce(b.endret_tid, b.opprettet_tid)>=:startAvDag and coalesce(b.endret_tid, b.opprettet_tid) < :nesteDag";

        String metricName = "avslag_daglig_v2";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behStatuser", Set.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode()))
            .setParameter("startAvDag", dato.atStartOfDay())
            .setParameter("nesteDag", dato.plusDays(1).atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> {
            String ytelseType = t.get(0, String.class);
            String saksnummer = t.get(1, String.class);
            String behandlingId = t.get(2, BigInteger.class).toString();
            String behandlingType = t.get(3, String.class);
            String behandlingResultatType = t.get(4, String.class);
            String vilkårType = t.get(5, String.class);
            String avslagKode = t.get(6, String.class);
            long tidsstempel = t.get(7, Timestamp.class).getTime();
            return SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", ytelseType,
                    "behandlingType", behandlingType,
                    "behandlingResultatType", behandlingResultatType,
                    "vilkårType", vilkårType,
                    "avslagKode", avslagKode),
                Map.of(
                    "saksnummer", saksnummer,
                    "behandlingId", behandlingId),
                tidsstempel);
        })
            .collect(Collectors.toList());

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> aksjonspunktStatistikk() {
        String sql = "select f.ytelse_type, a.aksjonspunkt_def as aksjonspunkt, a.aksjonspunkt_status," +
            " count(*) as antall" +
            " from aksjonspunkt a " +
            " inner join behandling b on b.id =a.behandling_id" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " where a.aksjonspunkt_status IN (:statuser)" +
            " group by 1, 2, 3";

        String metricName = "aksjonspunkt_per_ytelse_type_v3";
        String metricField = "totalt_antall";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("statuser", AKSJONSPUNKT_STATUSER.stream().collect(Collectors.toSet()));

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "aksjonspunkt", t.get(1, String.class),
                "aksjonspunkt_status", t.get(2, String.class)),
            Map.of(
                metricField, t.get(3, BigInteger.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden aksjonspunkt endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "aksjonspunkt", AKSJONSPUNKTER,
                "aksjonspunkt_status", AKSJONSPUNKT_STATUSER),
            Map.of(
                metricField, BigInteger.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> aksjonspunktStatistikkDaglig(LocalDate dato) {
        String sql = "select f.ytelse_type, f.saksnummer, b.id as behandling_id, a.aksjonspunkt_def as aksjonspunkt, " +
            "      a.aksjonspunkt_status as status,a.vent_aarsak, coalesce(a.endret_tid, a.opprettet_tid) as tid" +
            " from aksjonspunkt a " +
            " inner join behandling b on b.id =a.behandling_id" +
            " inner join fagsak f on f.id=b.fagsak_id" +
            " where a.aksjonspunkt_status IN (:statuser)"
            + " and coalesce(a.endret_tid, a.opprettet_tid)>=:startAvDag and coalesce(a.endret_tid, a.opprettet_tid) < :nesteDag";

        String metricName = "aksjonspunkt_daglig_v2";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("statuser", AKSJONSPUNKT_STATUSER.stream().collect(Collectors.toSet()))
            .setParameter("startAvDag", dato.atStartOfDay())
            .setParameter("nesteDag", dato.plusDays(1).atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> {
            String ytelseType = t.get(0, String.class);
            String saksnummer = t.get(1, String.class);
            String behandlingId = t.get(2, BigInteger.class).toString();
            String aksjonspunktKode = t.get(3, String.class);
            String aksjonspunktNavn = coalesce(AksjonspunktDefinisjon.kodeMap().getOrDefault(aksjonspunktKode, AksjonspunktDefinisjon.UNDEFINED).getNavn(), UDEFINERT);
            String aksjonspunktStatus = t.get(4, String.class);
            String venteÅrsak = coalesce(t.get(5, String.class), UDEFINERT);
            long tidsstempel = t.get(6, Timestamp.class).getTime();
            return SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", ytelseType,
                    "aksjonspunkt_status", aksjonspunktStatus,
                    "venteÅrsak", venteÅrsak,
                    "aksjonspunkt", aksjonspunktKode),
                Map.of(
                    "aksjonspunkt_navn", aksjonspunktNavn,
                    "saksnummer", saksnummer,
                    "behandlingId", behandlingId),
                tidsstempel);
        })
            .collect(Collectors.toList());

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> aksjonspunktVenteårsakStatistikk() {

        String sql = "select f.ytelse_type, a.aksjonspunkt_def, a.vent_aarsak, count(*) antall " +
            "         from aksjonspunkt a" +
            "         inner join behandling b on b.id =a.behandling_id " +
            "         inner join fagsak f on f.id=b.fagsak_id" +
            "         where a.vent_aarsak IS NOT NULL and a.vent_aarsak!='-'" +
            "         group by 1, 2, 3" +
            "         order by 1, 2, 3";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        String metricName = "aksjonspunkt_ytelse_type_vent_aarsak_v3";
        String metricField = "totalt_antall";

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "aksjonspunkt", t.get(1, String.class),
                "vent_aarsak", t.get(2, String.class)),
            Map.of(
                metricField, t.get(3, BigInteger.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden aksjonspunkt endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "aksjonspunkt", AKSJONSPUNKTER,
                "vent_aarsak", VENT_ÅRSAKER),
            Map.of(
                metricField, BigInteger.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;
    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> mottattDokumentStatistikk() {

        String sql = "select f.ytelse_type, m.type, count(*) " +
            "         from mottatt_dokument m" +
            "         inner join fagsak f on f.id = m.fagsak_id" +
            "         group by 1, 2";

        String metricName = "mottatt_dokument_v1";
        String metricField = "totalt_antall";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "type", t.get(1, String.class)),
            Map.of(
                "totalt_antall", t.get(2, BigInteger.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden fagsak endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "type", BREVKODER),
            Map.of(
                metricField, BigInteger.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> prosessTaskStatistikk() {

        // hardkoder statuser for bedre access plan for partisjon i db
        String sql = " select coalesce(f.ytelse_type, 'NONE') as ytelse_type, p.task_type, p.status, count(*) antall " +
            " from prosess_task_type t" +
            " inner join prosess_task p on p.task_type=t.kode and p.status in ('FEILET', 'VENTER_SVAR', 'KLAR')" +
            " left outer join fagsak_prosess_task fpt on fpt.prosess_task_id=p.id" +
            " left outer join fagsak f on f.id=fpt.fagsak_id" +
            " where p.status IN ('FEILET', 'VENTER_SVAR', 'KLAR')" +
            " group by 1, 2, 3" +
            " order by 1, 2, 3";

        String metricName = "prosess_task_" + PROSESS_TASK_VER;
        String metricField = "totalt_antall";

        NativeQuery<Tuple> queryType = (NativeQuery<Tuple>) entityManager.createNativeQuery("select kode from prosess_task_type", Tuple.class);
        var typer = queryType.getResultStream().map(t -> t.get(0, String.class)).collect(Collectors.toSet());

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class))); // forkaster dummy ytelse_type fra db
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
            toMap(
                "ytelse_type", t.get(0, String.class),
                "prosess_task_type", t.get(1, String.class),
                "status", t.get(2, String.class)),
            Map.of(
                metricField, t.get(3, BigInteger.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden aksjonspunkt endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "prosess_task_type", typer,
                "status", PROSESS_TASK_STATUSER),
            Map.of(
                metricField, BigInteger.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;
    }

    Collection<SensuEvent> prosessTaskFeilStatistikk() {
        String sql = "select coalesce(f.ytelse_type, 'NONE'), f.saksnummer, p.id, p.task_type, p.status, p.siste_kjoering_slutt_ts, p.siste_kjoering_feil_tekst, p.task_parametere"
            + " , p.blokkert_av, p.opprettet_tid, fpt.gruppe_sekvensnr"
            + " from prosess_task p " +
            " left outer join fagsak_prosess_task fpt ON fpt.prosess_task_id = p.id" +
            " left outer join fagsak f on f.id=fpt.fagsak_id" +
            " where ("
            + "       (p.status IN ('FEILET') AND p.siste_kjoering_feil_tekst IS NOT NULL)" // har feilet
            + "    OR (p.status IN ('KLAR', 'VETO') AND p.opprettet_tid < :ts AND (p.neste_kjoering_etter IS NULL OR p.neste_kjoering_etter < :ts2))" // har ligget med veto, klar lenge
            + "    OR (p.status IN ('VENTER_SVAR', 'SUSPENDERT') AND p.opprettet_tid < :ts )" // har ligget og ventet svar lenge
            + " )";

        String metricName = "prosess_task_feil_log_" + PROSESS_TASK_VER;
        LocalDateTime nå = LocalDateTime.now();
        
        @SuppressWarnings("unchecked")
        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("ts", nå.truncatedTo(ChronoUnit.DAYS))
            .setParameter("ts2", nå);

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class))); // forkaster dummy ytelse_type fra db

        long now = System.currentTimeMillis();

        Collection<SensuEvent> values = stream.map(t -> {
            String ytelseType = t.get(0, String.class);
            String saksnummer = t.get(1, String.class);
            String taskId = t.get(2, BigInteger.class).toString();
            String taskType = t.get(3, String.class);
            String status = t.get(4, String.class);
            Timestamp sistKjørt = t.get(5, Timestamp.class);
            long tidsstempel = sistKjørt == null ? now : sistKjørt.getTime();

            String sisteFeil = finnStacktraceStartFra(t.get(6, String.class), 500).orElse(UDEFINERT);
            String taskParams = t.get(7, String.class);

            BigInteger blokkertAvId = t.get(8, BigInteger.class);
            String blokkertAv = blokkertAvId == null ? null : blokkertAvId.toString();

            String opprettetTid = t.get(9, Timestamp.class).toInstant().toString();

            var gruppeSekvensnr = t.get(10, BigInteger.class);

            return SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelseType", coalesce(ytelseType, UDEFINERT),
                    "status", status,
                    "prosess_task_type", taskType),
                Map.of(
                    "taskId", taskId,
                    "saksnummer", coalesce(saksnummer, UDEFINERT),
                    "siste_feil", sisteFeil,
                    "task_parametere", coalesce(taskParams, UDEFINERT),
                    "blokkert_av", coalesce(blokkertAv, UDEFINERT),
                    "opprettet_tid", opprettetTid,
                    "gruppe_sekvensnr", gruppeSekvensnr == null ? UDEFINERT : gruppeSekvensnr.toString()),
                tidsstempel);
        })
            .collect(Collectors.toList());

        return values;
    }

    private static String coalesce(String str, String defValue) {
        return str != null ? str : defValue;
    }

    private static Optional<String> finnStacktraceStartFra(String sisteFeil, int maksLen) {
        boolean guessItsJson = sisteFeil != null && sisteFeil.startsWith("{");
        if (guessItsJson) {
            try {
                var feil = OM.readValue(sisteFeil, ProsessTaskFeil.class);
                var strFeil = feil.getStackTrace();
                return strFeil == null ? Optional.empty() : Optional.of(strFeil.substring(0, Math.min(maksLen, strFeil.length()))); // chop-chop
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Ugyldig json: " + sisteFeil, e);
            }
        }
        return Optional.empty();
    }

    /** Lager events med 0 målinger for alle kombinasjoner av oppgitte vektorer. */
    private Collection<SensuEvent> emptyEvents(String metricName, Map<String, Collection<String>> vectors, Map<String, Object> defaultVals) {
        List<Map<String, String>> matrix = new CombineLists<String>(vectors).toMap();
        return matrix.stream()
            .map(v -> SensuEvent.createSensuEvent(metricName, v, defaultVals))
            .collect(Collectors.toCollection(ArrayList::new));
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
}
