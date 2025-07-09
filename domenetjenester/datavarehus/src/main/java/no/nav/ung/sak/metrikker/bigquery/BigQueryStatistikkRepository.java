package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Dependent
public class BigQueryStatistikkRepository {

    private static final Logger log = LoggerFactory.getLogger(BigQueryStatistikkRepository.class);

    private static final String OBSOLETE_KODE = FagsakYtelseType.OBSOLETE.getKode();

    private final EntityManager entityManager;
    private final Set<String> taskTyper;

    @Inject
    public BigQueryStatistikkRepository(
            EntityManager entityManager,
            @Any Instance<ProsessTaskHandler> handlers
    ) {
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

    public Map<BigQueryTable, JSONObject> hentHyppigRapporterte() {
        LocalDate dag = LocalDate.now();
        Map<BigQueryTable, JSONObject> hyppigRapporterte = new HashMap<>();

        Tuple<BigQueryTable, JSONObject> fagsakStatusStatistikk = fagsakStatusStatistikk();
        hyppigRapporterte.put(fagsakStatusStatistikk.getElement1(), fagsakStatusStatistikk.getElement2());

        // TODO; behandlingStatusStatistikk
        // TODO: behandlingResultatStatistikk
        // TODO: prosessTaskStatistikk
        // TODO: mottattDokumentMedKildesystemStatistikk
        // TODO: aksjonspunktStatistikk
        // TODO: aksjonspunktStatistikkDaglig
        // TODO: avslagStatistikkDaglig
        // TODO: avslagStatistikk
        // TODO: prosessTaskFeilStatistikk

        return hyppigRapporterte;
    }

    public Map<BigQueryTable, JSONObject> hentDagligRapporterte() {
        // TODO: avslagStatistikk
        throw new UnsupportedOperationException("TODO: avslagStatistikk");
    }

    Tuple<BigQueryTable, JSONObject> fagsakStatusStatistikk() {
        BigQueryTable fagsakStatusTabell = BigQueryTable.FAGSAK_STATUS_TABELL_V1;
        String metricName = fagsakStatusTabell.getTableNavn();

        String sql = "SELECT jsonb_build_object(" +
                ":metricName, jsonb_agg(row_to_json(subQuery))) AS result " +
                "FROM (" +
                "  SELECT f.fagsak_status, count(*) as antall " +
                "  FROM fagsak f " +
                "  WHERE f.ytelse_type <> :obsoleteKode " +
                "  GROUP BY 1" +
                "  ORDER BY 1" +
                ") subQuery";

        String jsonResultat = (String) entityManager
                .createNativeQuery(sql)
                .setParameter("metricName", metricName)
                .setParameter("obsoleteKode", OBSOLETE_KODE)
                .getSingleResult(); // Henter resultatet som ett JSON-objekt

        return byggJsonObject(jsonResultat, fagsakStatusTabell);
    }

    private static Tuple<BigQueryTable, JSONObject> byggJsonObject(String jsonResultat, BigQueryTable bigQueryTable) {
        if (jsonResultat != null) {
            // Resultatet fra Postgres kommer som ett JSON-objekt
            return new Tuple<>(bigQueryTable, new JSONObject(jsonResultat));
        } else {
            // Fallback hvis ingen rader eller resultat null
            return new Tuple<>(bigQueryTable, new JSONObject().put(bigQueryTable.getTableNavn(), Collections.emptyList()));
        }
    }
}
