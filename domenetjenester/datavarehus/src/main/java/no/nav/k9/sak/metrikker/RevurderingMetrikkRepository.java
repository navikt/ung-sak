package no.nav.k9.sak.metrikker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.QueryTimeoutException;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.k9.felles.integrasjon.sensu.SensuEvent;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

/**
 * For innhenting av metrikker relatert til kvartalsmål for OKR
 */
@Dependent
public class RevurderingMetrikkRepository {

    private static final Logger log = LoggerFactory.getLogger(RevurderingMetrikkRepository.class);

    private EntityManager entityManager;



    @Inject
    public RevurderingMetrikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<SensuEvent> hentAlle() {
        LocalDate dag = LocalDate.now();

        List<SensuEvent> metrikker = new ArrayList<>();
        try {
            metrikker.addAll(timeCall(() -> antallAksjonspunktFordelingForRevurderingSisteSyvDager(dag), "antallAksjonspunktFordelingForRevurderingSisteSyvDager"));
        } catch (QueryTimeoutException e) {
            log.warn("Uthenting av antallAksjonspunktFordelingForRevurderingSisteSyvDager feiler", e);
        }
        try {
            metrikker.addAll(timeCall(() -> antallRevurderingMedAksjonspunktPrKodeSisteSyvDager(dag), "antallRevurderingMedAksjonspunktPrKodeSisteSyvDager"));
        } catch (QueryTimeoutException e) {
            log.warn("Uthenting av antallRevurderingMedAksjonspunktPrKodeSisteSyvDager feiler", e);
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
    Collection<SensuEvent> antallAksjonspunktFordelingForRevurderingSisteSyvDager(LocalDate dato) {
        String sql = "select ytelse_type, " +
            "antall_aksjonspunkter, " +
            "count(*) as antall_behandlinger from (" +
            "   select f.ytelse_type, b.id, " +
            "   count(a.aksjonspunkt_def) as antall_aksjonspunkter " +
            "   from behandling b" +
            "            inner join fagsak f on f.id=b.fagsak_id" +
            "            full outer join aksjonspunkt a on b.id = a.behandling_id " +
            "   where (a.aksjonspunkt_status is null or a.aksjonspunkt_status != 'AVBR') " +
            "   and b.avsluttet_dato is not null " +
            "   and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "   and b.behandling_type=:revurdering " +
            "   group by 1, 2) as statistikk_pr_behandling " +
            "group by 1, 2;";

        String metricName = "revurdering_antall_aksjonspunkt_fordeling";
        String metricField = "antall_behandlinger";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "antall_aksjonspunkter", t.get(1, Long.class).toString()),
                Map.of(metricField, t.get(3, Long.class).toString())))
            .collect(Collectors.toList());

        return values;

    }


    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallRevurderingMedAksjonspunktPrKodeSisteSyvDager(LocalDate dato) {
        String sql = "select f.ytelse_type, a.aksjonspunkt_def, count(*) as antall_behandlinger " +
            "from behandling b" +
            "         inner join fagsak f on f.id=b.fagsak_id" +
            "         inner join aksjonspunkt a on b.id = a.behandling_id " +
            "where a.aksjonspunkt_status != 'AVBR' " +
            "and b.avsluttet_dato is not null " +
            "and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "and b.behandling_type=:revurdering " +
            "group by 1, 2";

        String metricName = "revurdering_antall_behandlinger_pr_aksjonspunkt";
        String metricField = "antall_behandlinger";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "aksjonspunkt", t.get(1, Long.class).toString()),
                Map.of(metricField, t.get(2, Long.class).toString())))
            .collect(Collectors.toList());

        return values;

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
            // influxdb Point takler ikke null key eller value. skipper null verdier
            String v = args[i + 1];
            if (v != null) {
                map.put(args[i], v);
            }
        }
        return map;
    }
}
