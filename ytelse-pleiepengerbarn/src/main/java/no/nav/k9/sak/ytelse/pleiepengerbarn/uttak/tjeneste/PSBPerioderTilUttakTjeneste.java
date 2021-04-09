package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.vedtak.ekstern.UttakTilOverlappSjekkTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PSBPerioderTilUttakTjeneste implements UttakTilOverlappSjekkTjeneste {
    private UttakTjeneste uttakTjeneste;

    public PSBPerioderTilUttakTjeneste() {
        // CDI
    }

    @Inject
    public PSBPerioderTilUttakTjeneste(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public List<FagsakYtelseType> getYtelseTyperSomSjekkesMot() {
        return List.of(FagsakYtelseType.SYKEPENGER);
    }

    @Override
    public TreeSet<LocalDateSegment<Boolean>> hentInnvilgetUttaksplan(BehandlingReferanse ref) {
        return uttakTjeneste.hentUttaksplan(ref.getBehandlingUuid())
            .getPerioder()
            .entrySet()
            .stream()
            .filter(it -> it.getValue().getUtfall().equals(Utfall.OPPFYLT))
            .map(it -> new LocalDateSegment<>(it.getKey().getFom(), it.getKey().getTom(), Boolean.TRUE))
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
