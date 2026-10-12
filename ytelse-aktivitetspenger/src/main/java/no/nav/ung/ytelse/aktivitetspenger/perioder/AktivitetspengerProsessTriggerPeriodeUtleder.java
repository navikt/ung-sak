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
import java.util.Comparator;
import java.util.Set;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerProsessTriggerPeriodeUtleder implements ProsessTriggerPeriodeUtleder {

    private final ProsessTriggereRepository prosessTriggereRepository;
    private final AktivitetspengerSøknadsperiodeTjeneste aktivitetspengerSøknadsperiodeTjeneste;

    @Inject
    public AktivitetspengerProsessTriggerPeriodeUtleder(ProsessTriggereRepository prosessTriggereRepository, AktivitetspengerSøknadsperiodeTjeneste aktivitetspengerSøknadsperiodeTjeneste) {
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.aktivitetspengerSøknadsperiodeTjeneste = aktivitetspengerSøknadsperiodeTjeneste;
    }

    /**
     * Utleder tidslinje for perioder til vurdering basert på relevante triggere
     *
     * @param behandligId Long
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
            .map(p -> new LocalDateTimeline<>(finnPeriodeForBehandlingsårsak(behandligId, p), Set.of(p.getÅrsak())))
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::union))
            .orElse(LocalDateTimeline.empty());
    }

    private LocalDateInterval finnPeriodeForBehandlingsårsak(Long behandligId, Trigger p) {
        if (p.getÅrsak() == BehandlingÅrsakType.NY_SØKT_PERIODE) {
            // Behandlinger opprettet uten NY_SØKT_PERIODE (eks kontrollbehandling) vil ikke ha startdato ved tilbakehopp, så bruker triggerperioden som fallback
            return aktivitetspengerSøknadsperiodeTjeneste.utledPeriode(behandligId).stream()
                .filter(it -> it.getTomDato().isAfter(p.getPeriode().getFomDato()))
                .min(Comparator.naturalOrder())
                .orElse(p.getPeriode())
                .toLocalDateInterval();
        }

        return p.getPeriode().toLocalDateInterval();
    }

}
