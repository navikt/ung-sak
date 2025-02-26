package no.nav.ung.sak.perioder;

import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

@Dependent
public class ProsessTriggerPeriodeUtleder {

    // Prosesstriggere som er relevante. RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM håndteres i UtledPeriodeTilVurderingFraUngdomsprogram
    public static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER,
        BehandlingÅrsakType.RE_HENDELSE_DØD_BARN,
        BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
        BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS,
        BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT,
        BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE
    );
    private final ProsessTriggereRepository prosessTriggereRepository;
    private final UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste;

    @Inject
    public ProsessTriggerPeriodeUtleder(ProsessTriggereRepository prosessTriggereRepository, UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste) {
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.ungdomsytelseSøknadsperiodeTjeneste = ungdomsytelseSøknadsperiodeTjeneste;
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
            .map(p -> new LocalDateTimeline<>(finnPeriodeForBehandlingsårsak(behandligId, p, p.getÅrsak()), Set.of(p.getÅrsak())))
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::union))
            .orElse(LocalDateTimeline.empty());
    }

    private LocalDateInterval finnPeriodeForBehandlingsårsak(Long behandligId, Trigger p, BehandlingÅrsakType årsak) {
        // For nye søknader så vil triggerperioden være uendelig fordi vi ikke vet sluttdato ved oppretting av trigger,
        // så vi begresenser det her til søknadsperide
        if (årsak == BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE) {
            return ungdomsytelseSøknadsperiodeTjeneste.utledPeriode(behandligId).stream()
                .filter(it -> it.inkluderer(p.getPeriode().getFomDato())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Hadde startdato som ikke overlappet med søknadsperiode"))
                .toLocalDateInterval();

        }

        return p.getPeriode().toLocalDateInterval();
    }

}
