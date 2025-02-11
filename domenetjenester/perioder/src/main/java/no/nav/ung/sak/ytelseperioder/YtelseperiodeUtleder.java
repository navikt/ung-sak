package no.nav.ung.sak.ytelseperioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.Period;

@Dependent
public class YtelseperiodeUtleder {

    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public YtelseperiodeUtleder(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, BehandlingRepository behandlingRepository) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.behandlingRepository = behandlingRepository;
    }


    /** Utleder oppstykkede ytelseperioder
     * Ytelseperioder brukes til generering av tilkjent ytelse, rapporteringsperioder for inntekt og eventuelle kontrollperioder for inntekt
     * @param behandlingId Id for behandling
     * @return Oppstykket tidslinje for ytelse
     */
    public LocalDateTimeline<Boolean> utledYtelsestidslinje(Long behandlingId) {
        final var ungdomsprogramperioder = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
        final var fagsakTomDato = behandlingRepository.hentBehandling(behandlingId).getFagsak().getPeriode().getTomDato();
        return ungdomsprogramperioder.compress()
            .splitAtRegular(ungdomsprogramperioder.getMinLocalDate().withDayOfMonth(1), fagsakTomDato, Period.ofMonths(1));
    }

}
