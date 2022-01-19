package no.nav.folketrygdloven.beregningsgrunnlag.regulering;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ProsessTask(GReguleringKandidatUtledningTask.TASKTYPE)
@ApplicationScoped
public class GReguleringKandidatUtledningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "gregulering.kandidatUtledning";
    public static final String YTELSE_TYPE = "ytelseType";
    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String DRYRUN = "dryrun";

    private static final Logger log = LoggerFactory.getLogger(GReguleringKandidatUtledningTask.class);
    private GReguleringRepository gReguleringRepository;


    GReguleringKandidatUtledningTask() {
        // CDI
    }

    @Inject
    public GReguleringKandidatUtledningTask(GReguleringRepository gReguleringRepository) {
        this.gReguleringRepository = gReguleringRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var ytelseType = FagsakYtelseType.fromString(prosessTaskData.getPropertyValue(YTELSE_TYPE));
        var fomValue = prosessTaskData.getPropertyValue(PERIODE_FOM);
        var fom = LocalDate.parse(fomValue);
        var tomValue = prosessTaskData.getPropertyValue(PERIODE_TOM);
        var tom = LocalDate.parse(tomValue);
        var dryRunValue = prosessTaskData.getPropertyValue(DRYRUN);
        var dryRun = Boolean.parseBoolean(dryRunValue);

        if (erUgyldig(fom, tom)) {
            throw new IllegalArgumentException("Ugyldig fom eller tom " + fom + " - " + tom);
        }

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

        if (dryRun) {
            log.info("DRYRUN - Fant {} kandidater til g-regulering for '{}' og perioden '{}'.", gReguleringRepository.dryRun(ytelseType, periode), ytelseType, periode);
        } else {
            var antall = gReguleringRepository.startGReguleringForPeriode(ytelseType, periode, fomValue, tomValue);
            log.info("Fant {} kandidater til g-regulering for '{}' og perioden '{}'. Starter vurdering", antall, ytelseType, periode);
        }
    }

    private boolean erUgyldig(LocalDate fom, LocalDate tom) {
        return fom.getYear() != tom.getYear();
    }
}
