package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.vedtak.ekstern.UttakTilOverlappSjekkTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OMPPerioderTilUttakTilOverlappSjekkTjeneste implements UttakTilOverlappSjekkTjeneste {

    private ÅrskvantumTjeneste årskvantumTjeneste;

    OMPPerioderTilUttakTilOverlappSjekkTjeneste() {
        // CDI
    }

    @Inject
    OMPPerioderTilUttakTilOverlappSjekkTjeneste(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public List<FagsakYtelseType> getYtelseTyperSomSjekkesMot() {
        return List.of(FagsakYtelseType.SYKEPENGER);
    }

    public TreeSet<LocalDateSegment<Boolean>> hentInnvilgetUttaksplan(BehandlingReferanse ref) {
        return årskvantumTjeneste.hentFullUttaksplan(ref.getSaksnummer())
            .getAktiviteter()
            .stream()
            .map(Aktivitet::getUttaksperioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getUtfall().equals(Utfall.INNVILGET))
            .map(Uttaksperiode::getPeriode)
            .map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), Boolean.TRUE))
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
