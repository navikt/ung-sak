package no.nav.ung.sak.ytelseperioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.Period;
import java.util.List;
import java.util.Objects;

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
    public LocalDateTimeline<YtelsesperiodeDefinisjon> utledYtelsestidslinje(Long behandlingId) {
        final var ungdomsprogramperioder = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
        final var fagsak = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        final var fagsakPeriode = fagsak.getPeriode();
        return ungdomsprogramperioder.intersection(new LocalDateTimeline<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), true))
            .compress()
            .splitAtRegular(ungdomsprogramperioder.getMinLocalDate().withDayOfMonth(1), fagsakPeriode.getTomDato(), Period.ofMonths(1))
            .map(it -> List.of(new LocalDateSegment<>(it.getLocalDateInterval(), new YtelsesperiodeDefinisjon(it.getFom().getMonth().name()))));
    }


}
