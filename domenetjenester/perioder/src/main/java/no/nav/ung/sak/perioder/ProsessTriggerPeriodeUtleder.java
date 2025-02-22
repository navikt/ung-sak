package no.nav.ung.sak.perioder;

import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;

@Dependent
public class ProsessTriggerPeriodeUtleder {

    // Prosesstriggere som er relevante. RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM håndteres i UtledPeriodeTilVurderingFraUngdomsprogram
    public static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER,
        BehandlingÅrsakType.RE_HENDELSE_DØD_BARN,
        BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
        BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS,
        BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT,
        BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER
    );
    private final ProsessTriggereRepository prosessTriggereRepository;


    @Inject
    public ProsessTriggerPeriodeUtleder(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    /**
     * Utleder tidslinje for perioder til vurdering basert på relevante triggere
     *
     * @param behandligId BehandlingId
     * @return Tidslinje for perioder til vurdering
     */
    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledTidslinje(Long behandligId) {
        return prosessTriggereRepository.hentGrunnlag(behandligId)
            .stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Collection::stream)
            .filter(it -> RELEVANTE_ÅRSAKER.contains(it.getÅrsak()))
            .map(p -> new LocalDateTimeline<>(p.getPeriode().toLocalDateInterval(), Set.of(p.getÅrsak())))
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::union))
            .orElse(LocalDateTimeline.empty());
    }

}
