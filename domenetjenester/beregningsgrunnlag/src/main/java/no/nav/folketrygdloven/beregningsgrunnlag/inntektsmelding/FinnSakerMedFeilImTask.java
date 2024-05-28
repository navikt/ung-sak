package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@ProsessTask(FinnSakerMedFeilImTask.TASKTYPE)
@ApplicationScoped
public class FinnSakerMedFeilImTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "forvaltning.feil.inntektsmelding";
    public static final String YTELSE_TYPE = "ytelseType";
    public static final String PERIODE_FOM = "fom";
    private static final Logger log = LoggerFactory.getLogger(FinnSakerMedFeilImTask.class);

    private EntityManager entityManager;
    private FinnPerioderMedEndringVedFeilInntektsmelding finnPerioderMedEndringVedFeilInntektsmelding;


    public FinnSakerMedFeilImTask() {
    }

    @Inject
    public FinnSakerMedFeilImTask(EntityManager entityManager, FinnPerioderMedEndringVedFeilInntektsmelding finnPerioderMedEndringVedFeilInntektsmelding) {
        this.entityManager = entityManager;
        this.finnPerioderMedEndringVedFeilInntektsmelding = finnPerioderMedEndringVedFeilInntektsmelding;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var ytelseType = FagsakYtelseType.fromString(prosessTaskData.getPropertyValue(YTELSE_TYPE));
        var fomValue = prosessTaskData.getPropertyValue(PERIODE_FOM);
        var fom = LocalDate.parse(fomValue);

        Query query = entityManager.createNativeQuery(
            "SELECT DISTINCT ON (b.fagsak_id) b.* from behandling b " +
                "inner join fagsak f on f.id = b.fagsak_id " +
                "where b.opprettet_tid >= :OPPRETTET_FOM " +
                "and b.avsluttet_dato is not null " +
                "and b.behandling_type = 'BT-004' " +
                "and f.ytelse_type = :YTELSE " +
                "order by b.fagsak_id, b.opprettet_tid desc",
            Behandling.class);
        query.setParameter("OPPRETTET_FOM", fom.atStartOfDay());
        query.setParameter("YTELSE", ytelseType.getKode());

        List<Behandling> behandlinger = query.getResultList();

        var relevanteEndringerPrBehandling = behandlinger.stream()
            .collect(Collectors.toMap(Behandling::getId, t -> {
                var behandlingReferanse = BehandlingReferanse.fra(t);
                return finnPerioderMedEndringVedFeilInntektsmelding.finnPerioderForEndringDersomFeilInntektsmeldingBrukes(behandlingReferanse, fom);
            }));

        var behandlingerMedEndringer = relevanteEndringerPrBehandling.entrySet().stream().filter(e -> e.getValue().isPresent())
            .toList();

        log.info("Fant følgende behandlinger med feil inntektsmelding: " + behandlingerMedEndringer);

    }
}
