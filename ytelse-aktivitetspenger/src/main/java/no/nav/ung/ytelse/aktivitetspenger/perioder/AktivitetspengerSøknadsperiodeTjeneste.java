package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.søknad.TidsserieUtils;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.function.Function;

@Dependent
public class AktivitetspengerSøknadsperiodeTjeneste {

    private final StartdatoRepository startdatoRepository;

    @Inject
    public AktivitetspengerSøknadsperiodeTjeneste(StartdatoRepository startdatoRepository) {
        this.startdatoRepository = startdatoRepository;
    }

    /**
     * Finner søknadsperioder som har kommet inn i denne behandlingen
     *
     * @param behandlingId BehandlingId
     * @return Relevante søknadsperioder for denne behandlingen
     */
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        return TidslinjeUtil.tilDatoIntervallEntiteter(finnTidslinje(behandlingId, StartdatoGrunnlag::getRelevanteStartdatoer));
    }


    /**
     * Finner søknadstidslinje som har kommet inn i denne behandlingen
     *
     * @param behandlingId BehandlingId
     * @return Relevante søknadsperioder for denne behandlingen
     */
    public LocalDateTimeline<Boolean> utledTidslinje(Long behandlingId) {
        return finnTidslinje(behandlingId, StartdatoGrunnlag::getRelevanteStartdatoer);
    }

    private LocalDateTimeline<Boolean> finnTidslinje(Long behandlingId,
                                                     Function<StartdatoGrunnlag, Startdatoer> finnPeriodeHolder) {
        var startdatoer = startdatoRepository.hentGrunnlag(behandlingId).map(finnPeriodeHolder);

        if (startdatoer.isEmpty() || startdatoer.get().getStartdatoer().isEmpty()) {
            return LocalDateTimeline.empty();
        } else {
            return startdatoer.map(Startdatoer::getStartdatoer)
                .orElse(Collections.emptySet())
                .stream()
                .map(SøktStartdato::getStartdato)
                .map(AktivitetspengerSøknadsperiodeTjeneste::tidslinjeFraVirkningstidspunkt)
                .reduce(LocalDateTimeline::crossJoin)
                .orElse(LocalDateTimeline.empty());
        }
    }

    public static LocalDateTimeline<Boolean> tidslinjeFraVirkningstidspunkt(LocalDate virkningstidspunkt){
        LocalDate tomDato = virkningstidspunkt.plusWeeks(52).minusDays(1);
        while (tomDato.getDayOfWeek() == DayOfWeek.SATURDAY || tomDato.getDayOfWeek() == DayOfWeek.SUNDAY){
            tomDato = tomDato.minusDays(1);
        }
        return new LocalDateTimeline<>(virkningstidspunkt, tomDato, true);
    }

}
