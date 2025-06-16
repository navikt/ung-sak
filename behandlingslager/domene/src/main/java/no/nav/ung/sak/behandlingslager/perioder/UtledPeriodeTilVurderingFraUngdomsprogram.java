package no.nav.ung.sak.behandlingslager.perioder;

import java.util.Collection;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

@Dependent
public class UtledPeriodeTilVurderingFraUngdomsprogram {

    private final ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    public UtledPeriodeTilVurderingFraUngdomsprogram(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    public LocalDateTimeline<Boolean> finnTidslinje(Long behandlingId) {
        var tidslinjeFraOpphørshendelser = prosessTriggereRepository.hentGrunnlag(behandlingId)
            .stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Collection::stream)
            .filter(t -> t.getÅrsak().equals(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM))
            .map(Trigger::getPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
        return tidslinjeFraOpphørshendelser.compress();
    }

}
