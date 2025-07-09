package no.nav.ung.sak.metrikker;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.k9.felles.integrasjon.sensu.SensuEvent;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import org.hibernate.QueryTimeoutException;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.ung.sak.metrikker.MetrikkUtils.UDEFINERT;
import static no.nav.ung.sak.metrikker.MetrikkUtils.coalesce;

@Dependent
public class StatistikkRepository {

    private static final Logger log = LoggerFactory.getLogger(StatistikkRepository.class);

    private EntityManager entityManager;
    private final Set<String> taskTyper;

    @Inject
    public StatistikkRepository(EntityManager entityManager, @Any Instance<ProsessTaskHandler> handlers) {
        this.entityManager = entityManager;
        this.taskTyper = handlers.stream()
            .map(this::extractClass)
            .map(it -> it.getAnnotation(ProsessTask.class).value())
            .collect(Collectors.toSet());
    }

    private Class<?> extractClass(ProsessTaskHandler bean) {
        if (!bean.getClass().isAnnotationPresent(ProsessTask.class) && bean instanceof TargetInstanceProxy<?> tip) {
            return tip.weld_getTargetInstance().getClass();
        } else {
            return bean.getClass();
        }
    }

    public List<SensuEvent> hentHyppigRapporterte() {
        LocalDate dag = LocalDate.now();

        List<SensuEvent> metrikker = new ArrayList<>();
        metrikker.addAll(timeCall(this::fagsakStatusStatistikk, "fagsakStatusStatistikk"));
        metrikker.addAll(timeCall(this::behandlingStatusStatistikk, "behandlingStatusStatistikk"));
        metrikker.addAll(timeCall(this::behandlingResultatStatistikk, "behandlingResultatStatistikk"));
        metrikker.addAll(timeCall(this::prosessTaskStatistikk, "prosessTaskStatistikk"));
        metrikker.addAll(timeCall(this::mottattDokumentStatistikk, "mottattDokumentStatistikk"));
        metrikker.addAll(timeCall(this::mottattDokumentMedKildesystemStatistikk, "mottattDokumentMedKildesystemStatistikk"));
        metrikker.addAll(timeCall(this::aksjonspunktStatistikk, "aksjonspunktStatistikk"));
        metrikker.addAll(timeCall(() -> aksjonspunktStatistikkDaglig(dag), "aksjonspunktStatistikkDaglig"));
        try {
            metrikker.addAll(timeCall(() -> avslagStatistikkDaglig(dag), "avslagStatistikkDaglig"));
        } catch (QueryTimeoutException e) {
            log.warn("Uthenting av avslagStatistikkDaglig feiler", e);
        }
        metrikker.addAll(timeCall(this::prosessTaskFeilStatistikk, "prosessTaskFeilStatistikk"));
        return metrikker;
    }

    public List<SensuEvent> hentDagligRapporterte() {
        List<SensuEvent> metrikker = new ArrayList<>();
        try {
            metrikker.addAll(timeCall(this::avslagStatistikk, "avslagStatistikk"));
        } catch (QueryTimeoutException e) {
            log.warn("Uthenting av avslagsStatistikk feiler", e);
        }
        return metrikker;
    }

    private Collection<SensuEvent> timeCall(Supplier<Collection<SensuEvent>> collectionSupplier, String function) {
        var start = System.currentTimeMillis();
        var sensuEvents = collectionSupplier.get();
        var slutt = System.currentTimeMillis();

        log.info("{} benyttet {} ms", function, (slutt - start));

        return sensuEvents;
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
            Map.of("totalt_antall", t.get(2, Long.class)))).collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden fagsak endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", MetrikkUtils.YTELSER,
                "fagsak_status", MetrikkUtils.FAGSAK_STATUS),
            Map.of(
                metricField, 0L));

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
                    metricField1, t.get(3, Long.class),
                    metricField2, t.get(4, Long.class),
                    metricField3, t.get(5, Long.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden aksjonspunkt endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", MetrikkUtils.YTELSER,
                "behandling_type", MetrikkUtils.BEHANDLING_TYPER,
                "behandling_resultat_type", MetrikkUtils.BEHANDLING_RESULTAT_TYPER),
            Map.of(
                metricField1, 0L,
                metricField2, 0L,
                metricField3, 0L));

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
                    metricField, t.get(3, Long.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden behandling endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", MetrikkUtils.YTELSER,
                "behandling_type", MetrikkUtils.BEHANDLING_TYPER,
                "behandling_status", MetrikkUtils.BEHANDLING_STATUS),
            Map.of(
                metricField, 0L));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    /**
     * trenger ikke sette 0 verdier for kombinasjoner som ikke fins, da dette ser kun på slutt resultater (vedtatte behandlinger).
     */
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
            .setHint(QueryHints.JAKARTA_SPEC_HINT_TIMEOUT, 40000)
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
                    metricField, t.get(5, Long.class))))
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
            .setHint(QueryHints.JAKARTA_SPEC_HINT_TIMEOUT, 30000)
            .setParameter("behStatuser", Set.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode()))
            .setParameter("startAvDag", dato.atStartOfDay())
            .setParameter("nesteDag", dato.plusDays(1).atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> {
                String ytelseType = t.get(0, String.class);
                String saksnummer = t.get(1, String.class);
                String behandlingId = t.get(2, Long.class).toString();
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
            .setParameter("statuser", MetrikkUtils.AKSJONSPUNKT_STATUSER.stream().collect(Collectors.toSet()));

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "aksjonspunkt", t.get(1, String.class),
                    "aksjonspunkt_status", t.get(2, String.class)),
                Map.of(
                    metricField, t.get(3, Number.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden aksjonspunkt endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", MetrikkUtils.YTELSER,
                "aksjonspunkt", MetrikkUtils.AKSJONSPUNKTER,
                "aksjonspunkt_status", MetrikkUtils.AKSJONSPUNKT_STATUSER),
            Map.of(
                metricField, 0L));

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
            .setParameter("statuser", MetrikkUtils.AKSJONSPUNKT_STATUSER.stream().collect(Collectors.toSet()))
            .setParameter("startAvDag", dato.atStartOfDay())
            .setParameter("nesteDag", dato.plusDays(1).atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> {
                String ytelseType = t.get(0, String.class);
                String saksnummer = t.get(1, String.class);
                String behandlingId = t.get(2, Long.class).toString();
                String aksjonspunktKode = t.get(3, String.class);
                String aksjonspunktNavn = coalesce(AksjonspunktDefinisjon.kodeMap().getOrDefault(aksjonspunktKode, AksjonspunktDefinisjon.UNDEFINED).getNavn(), UDEFINERT);
                String aksjonspunktStatus = t.get(4, String.class);
                String venteÅrsak = coalesce(t.get(5, String.class), UDEFINERT);
                long tidsstempel = t.get(6, Timestamp.class).getTime();

                return SensuEvent.createSensuEvent(metricName,
                    toMap(
                        "ytelse_type", ytelseType,
                        "aksjonspunkt_status", aksjonspunktStatus,
                        "vente_arsak", venteÅrsak,
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

    /**
     * Denne har ikke info om kildesystem. Bruk {@link #mottattDokumentMedKildesystemStatistikk()} i stedet.
     * Siden metrikkene går ett år tilbake i tid, venter vi med å fjerne den til mottattDokumentMedKildesystemStatistikk
     * har mer data.
     *
     * Kan fjernes etter 17-10-2025.
     *
     * @deprecated bruk {@link #mottattDokumentMedKildesystemStatistikk()} i stedet.
     */
    @Deprecated
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
                    "totalt_antall", t.get(2, Long.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden fagsak endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", MetrikkUtils.YTELSER,
                "type", MetrikkUtils.BREVKODER),
            Map.of(
                metricField, 0L));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> mottattDokumentMedKildesystemStatistikk() {

        String sql = """
            select f.ytelse_type, m.type, m.kildesystem, count(*)
            from mottatt_dokument m
                inner join fagsak f on f.id = m.fagsak_id
            group by 1, 2,3;
            """;
        String metricName = "mottatt_dokument_med_kilde_v1";
        String metricField = "totalt_antall";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);
        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "type", t.get(1, String.class),
                    "kildesystem", t.get(2, String.class)),
                Map.of(
                    "totalt_antall", t.get(3, Long.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden fagsak endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", MetrikkUtils.YTELSER,
                "type", MetrikkUtils.BREVKODER),
            Map.of(
                metricField, 0L));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;
    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> prosessTaskStatistikk() {

        // hardkoder statuser for bedre access plan for partisjon i db
        String sql = " select coalesce(f.ytelse_type, 'NONE') as ytelse_type, p.task_type, p.status, count(*) antall " +
            " from prosess_task p" +
            " left outer join fagsak_prosess_task fpt on fpt.prosess_task_id=p.id" +
            " left outer join fagsak f on f.id=fpt.fagsak_id" +
            " where p.status IN (:statuser)" +
            " group by 1, 2, 3" +
            " order by 1, 2, 3";

        String metricName = "prosess_task_" + MetrikkUtils.PROSESS_TASK_VER;
        String metricField = "totalt_antall";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("statuser", MetrikkUtils.PROSESS_TASK_STATUSER);

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class))); // forkaster dummy ytelse_type fra db
        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "prosess_task_type", t.get(1, String.class),
                    "status", t.get(2, String.class)),
                Map.of(
                    metricField, t.get(3, Number.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        /* siden aksjonspunkt endrer status må vi ta hensyn til at noen verdier vil gå til 0, ellers vises siste verdi i stedet. */
        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", MetrikkUtils.YTELSER,
                "prosess_task_type", taskTyper,
                "status", MetrikkUtils.PROSESS_TASK_STATUSER),
            Map.of(
                metricField, 0L));

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

        String metricName = "prosess_task_feil_log_" + MetrikkUtils.PROSESS_TASK_VER;
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
                String taskId = t.get(2, Number.class).toString();
                String taskType = t.get(3, String.class);
                String status = t.get(4, String.class);
                Timestamp sistKjørt = t.get(5, Timestamp.class);
                long tidsstempel = sistKjørt == null ? now : sistKjørt.getTime();

                String sisteFeil = MetrikkUtils.finnStacktraceStartFra(t.get(6, String.class), 500).orElse(UDEFINERT);
                String taskParams = t.get(7, String.class);

                Number blokkertAvId = t.get(8, Number.class);
                String blokkertAv = blokkertAvId == null ? null : blokkertAvId.toString();

                String opprettetTid = t.get(9, Timestamp.class).toInstant().toString();

                var gruppeSekvensnr = t.get(10, Long.class);

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

    /**
     * Lager events med 0 målinger for alle kombinasjoner av oppgitte vektorer.
     */
    private Collection<SensuEvent> emptyEvents(String metricName, Map<String, Collection<String>> vectors, Map<String, Object> defaultVals) {
        List<Map<String, String>> matrix = new CombineLists<String>(vectors).toMap();
        return matrix.stream()
            .map(v -> SensuEvent.createSensuEvent(metricName, v, defaultVals))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Map.of() takler ikke null verdier, så vi lager vår egen variant.
     */
    private static Map<String, String> toMap(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Må ha partall antall argumenter, fikk: " + Arrays.asList(args));
        }
        var map = new HashMap<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            // Influxdb Point takler ikke null key eller value. Skipper null verdier
            String v = args[i + 1];
            if (v != null) {
                map.put(args[i], v);
            }
        }
        return map;
    }
}
