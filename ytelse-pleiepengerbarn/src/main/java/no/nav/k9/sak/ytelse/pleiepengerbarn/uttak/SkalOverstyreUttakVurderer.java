package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
public class SkalOverstyreUttakVurderer {

    private OverstyrUttakRepository overstyrUttakRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;


    public SkalOverstyreUttakVurderer() {
    }

    @Inject
    public SkalOverstyreUttakVurderer(OverstyrUttakRepository overstyrUttakRepository, @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    public boolean skalOverstyreUttak(BehandlingReferanse behandlingReferanse) {
        var overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandlingReferanse.getBehandlingId());
        var periodeTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        var perioderTilVurdering = periodeTjeneste.utledFraDefinerendeVilkår(behandlingReferanse.getBehandlingId());
        var tidslinjeTilVurdering = new LocalDateTimeline<>(perioderTilVurdering.stream().map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), Boolean.TRUE)).toList());
        return !overstyrtUttak.intersection(tidslinjeTilVurdering).isEmpty();
    }

}
