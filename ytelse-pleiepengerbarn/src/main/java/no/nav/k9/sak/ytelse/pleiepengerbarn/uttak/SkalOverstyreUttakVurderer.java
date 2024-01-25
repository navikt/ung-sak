package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

public class SkalOverstyreUttakVurderer {

    private final OverstyrUttakRepository overstyrUttakRepository;
    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    public SkalOverstyreUttakVurderer(OverstyrUttakRepository overstyrUttakRepository, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    public boolean skalOverstyreUttak(BehandlingReferanse behandlingReferanse) {
        var overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandlingReferanse.getBehandlingId());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandlingReferanse.getBehandlingId());
        var tidslinjeTilVurdering = new LocalDateTimeline<>(perioderTilVurdering.stream().map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), Boolean.TRUE)).toList());
        return overstyrtUttak.intersects(tidslinjeTilVurdering);
    }

}
