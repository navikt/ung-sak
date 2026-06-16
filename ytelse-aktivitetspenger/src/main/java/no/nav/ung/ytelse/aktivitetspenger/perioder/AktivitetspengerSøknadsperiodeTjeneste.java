package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;

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
                .map(d -> new LocalDateTimeline<>(d, d.plusWeeks(52).minusDays(1), true))
                .reduce(LocalDateTimeline::crossJoin)
                .orElse(LocalDateTimeline.empty());
        }
    }


}
