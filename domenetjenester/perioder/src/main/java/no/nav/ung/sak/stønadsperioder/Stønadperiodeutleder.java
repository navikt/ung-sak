package no.nav.ung.sak.stønadsperioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.Period;

@Dependent
public class Stønadperiodeutleder {

    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public Stønadperiodeutleder(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }


    /** Utleder stønadsperioder
     * Stønadsperioder brukes til generering av tilkjent ytelse, rapporteringsperioder for inntekt og eventuelle kontrollperioder for inntekt
     * @param behandlingId
     * @return
     */
    public LocalDateTimeline<Boolean> utledStønadstidslinje(Long behandlingId) {
        final var ungdomsprogramperioder = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
        return ungdomsprogramperioder.compress()
            .splitAtRegular(ungdomsprogramperioder.getMinLocalDate().withDayOfMonth(1), ungdomsprogramperioder.getMaxLocalDate(), Period.ofMonths(1));
    }

}
