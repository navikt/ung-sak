package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

public class SkalOverstyreUttakVurderer {

    private final OverstyrUttakRepository overstyrUttakRepository;
    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private final BehandlingRepository behandlingRepository;

    public SkalOverstyreUttakVurderer(OverstyrUttakRepository overstyrUttakRepository, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, AksjonspunktRepository aksjonspunktRepository, BehandlingRepository behandlingRepository) {
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public boolean skalOverstyreUttak(BehandlingReferanse behandlingReferanse) {
        var overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandlingReferanse.getBehandlingId());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandlingReferanse.getBehandlingId());
        var tidslinjeTilVurdering = new LocalDateTimeline<>(perioderTilVurdering.stream().map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), Boolean.TRUE)).toList());
        var harOverstyringForPeriode = overstyrtUttak.intersects(tidslinjeTilVurdering);
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        var harLøstOverlappendeSakerAksjonspunkt = behandling.getAksjonspunkter().stream().anyMatch(ap -> ap.erUtført() && ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_OVERLAPPENDE_SØSKENSAKER));
        return harOverstyringForPeriode && !harLøstOverlappendeSakerAksjonspunkt;
    }

}
