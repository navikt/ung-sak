package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søsken;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class AksjonspunktutlederSøskensak {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private FinnTidslinjeForOverlappendeSøskensaker finnTidslinjeForOverlappendeSøskensaker;

    @Inject
    public AksjonspunktutlederSøskensak(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                        FinnTidslinjeForOverlappendeSøskensaker finnTidslinjeForOverlappendeSøskensaker) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.finnTidslinjeForOverlappendeSøskensaker = finnTidslinjeForOverlappendeSøskensaker;
    }

    public boolean skalHaAksjonspunktForSøskensak(BehandlingReferanse behandlingReferanse) {
        return !finnTidslinjeForOverlapp(behandlingReferanse).isEmpty();
    }

    private LocalDateTimeline<Set<Saksnummer>> finnTidslinjeForOverlapp(BehandlingReferanse behandlingReferanse) {
        var vilkårsPerioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        var perioderTilVurdering = vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandlingReferanse.getBehandlingId());
        var tidslinjeMedOverlappBlantFagsaker = finnTidslinjeForOverlappendeSøskensaker.finnTidslinje(behandlingReferanse.getAktørId(), behandlingReferanse.getFagsakYtelseType());
        var tidslinjeForOverlappMedDenneFagsaken = tidslinjeMedOverlappBlantFagsaker.filterValue(v -> v.contains(behandlingReferanse.getSaksnummer()));
        var tidslinjeForOverlappMedPerioderTilVurdering = tidslinjeForOverlappMedDenneFagsaken.intersection(TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering));
        return tidslinjeForOverlappMedPerioderTilVurdering;
    }


}
