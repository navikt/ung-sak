package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

import java.util.Collection;
import java.util.Set;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerProsessTriggerPeriodeUtleder implements ProsessTriggerPeriodeUtleder {

    private final ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    public AktivitetspengerProsessTriggerPeriodeUtleder(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    /**
     * Utleder tidslinje for perioder til vurdering basert på relevante triggere
     *
     * @param behandligId BehandlingId
     * @return Tidslinje for perioder til vurdering
     */
    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledTidslinje(Long behandligId) {
        final var triggere = prosessTriggereRepository.hentGrunnlag(behandligId)
            .stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Collection::stream)
            .toList();
        return triggere
            .stream()
            .map(p -> new LocalDateTimeline<>(finnPeriodeForBehandlingsårsak(p), Set.of(p.getÅrsak())))
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::union))
            .orElse(LocalDateTimeline.empty());
    }

    private LocalDateInterval finnPeriodeForBehandlingsårsak(Trigger p) {
        return p.getPeriode().toLocalDateInterval();
    }

}
