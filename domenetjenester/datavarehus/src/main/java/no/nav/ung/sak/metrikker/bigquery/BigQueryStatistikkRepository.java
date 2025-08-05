package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import org.hibernate.query.NativeQuery;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentHyppigRapporterte() {
        List<Tuple<BigQueryTabell<?>, Collection<?>>> hyppigRapporterte = new ArrayList<>();

        Collection<FagsakStatusRecord> fagsakStatusStatistikk = fagsakStatusStatistikk();
        hyppigRapporterte.add(new Tuple<>(Tabeller.FAGSAK_STATUS_V2, fagsakStatusStatistikk));

        // TODO: satsStatistikk
        // TODO: barnetilleggStatistikk
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

    public Map<BigQueryTabell, JSONObject> hentDagligRapporterte() {
        // TODO: avslagStatistikk
        throw new UnsupportedOperationException("TODO: avslagStatistikk");
    }

    Collection<FagsakStatusRecord> fagsakStatusStatistikk() {
        String sql = "select f.fagsak_status, count(*) as antall" +
            "         from fagsak f" +
            "         where f.ytelse_type <> :obsoleteKode " +
            "         group by 1" +
            "         order by 1";

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("obsoleteKode", OBSOLETE_KODE)
            .getResultStream();

        return stream.map(t -> {
            String fagsakStatus = t.get(0, String.class);
            Long totaltAntall = t.get(1, Long.class);

            return new FagsakStatusRecord(BigDecimal.valueOf(totaltAntall), FagsakStatus.fraKode(fagsakStatus), ZonedDateTime.now());
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
