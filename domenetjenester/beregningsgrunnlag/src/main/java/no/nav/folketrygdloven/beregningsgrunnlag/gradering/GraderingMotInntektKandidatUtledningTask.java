package no.nav.folketrygdloven.beregningsgrunnlag.gradering;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ProsessTask(GraderingMotInntektKandidatUtledningTask.TASKTYPE)
@ApplicationScoped
public class GraderingMotInntektKandidatUtledningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "gradering.kandidatUtledning";
    public static final String DRYRUN = "dryrun";

    private static final Logger log = LoggerFactory.getLogger(GraderingMotInntektKandidatUtledningTask.class);
    public static final LocalDate FOM_DATO_INNTEKT_GRADERING = LocalDate.of(2023, 4, 1);
    private InntektGraderingRepository inntektGraderingRepository;


    GraderingMotInntektKandidatUtledningTask() {
        // CDI
    }

    @Inject
    public GraderingMotInntektKandidatUtledningTask(InntektGraderingRepository inntektGraderingRepository) {
        this.inntektGraderingRepository = inntektGraderingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRunValue = prosessTaskData.getPropertyValue(DRYRUN);
        var dryRun = Boolean.parseBoolean(dryRunValue);

        if (dryRun) {
            log.info("DRYRUN - Fant {} kandidater til gradering mot inntekt.", inntektGraderingRepository.dryRun(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FOM_DATO_INNTEKT_GRADERING));
        } else {
            var antall = inntektGraderingRepository.startInntektGraderingForPeriode(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FOM_DATO_INNTEKT_GRADERING);
            log.info("Fant {} kandidater til gradering mot inntekt. Starter vurdering", antall);
        }
    }

}
