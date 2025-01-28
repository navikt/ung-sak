package no.nav.ung.sak.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

import java.util.Collection;
import java.util.Set;

@Dependent
public class ProsessTriggerPeriodeUtleder {

    // Prosesstriggere som er relevante. RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM håndteres i UtledPeriodeTilVurderingFraUngdomsprogram
    public static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER,
        BehandlingÅrsakType.RE_HENDELSE_DØD_BARN,
        BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
        BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS
    );
    private final ProsessTriggereRepository prosessTriggereRepository;


    @Inject
    public ProsessTriggerPeriodeUtleder(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    LocalDateTimeline<Boolean> utledTidslinjeFraProsesstriggere(Long behandligId) {
        return prosessTriggereRepository.hentGrunnlag(behandligId)
            .stream()
            .map(gr -> gr.getTriggere())
            .flatMap(Collection::stream)
            .filter(it -> RELEVANTE_ÅRSAKER.contains(it.getÅrsak()))
            .map(Trigger::getPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

}
