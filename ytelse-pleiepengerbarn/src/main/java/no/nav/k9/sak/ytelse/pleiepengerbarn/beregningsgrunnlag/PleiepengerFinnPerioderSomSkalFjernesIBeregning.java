package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnPerioderSomSkalFjernesIBeregning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.FinnPerioderMedAvslåtteInngangsvilkårForBeregning;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
public class PleiepengerFinnPerioderSomSkalFjernesIBeregning implements FinnPerioderSomSkalFjernesIBeregning {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    public PleiepengerFinnPerioderSomSkalFjernesIBeregning() {
    }

    @Inject
    public PleiepengerFinnPerioderSomSkalFjernesIBeregning(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public Set<DatoIntervallEntitet> finnPerioderSomSkalFjernes(Vilkårene vilkårene, BehandlingReferanse behandlingReferanse, Set<DatoIntervallEntitet> aktuelleVilkårsperioder) {
        var perioderMedAvslåtteInngangsvilkår = finnPerioderMedAvslåtteInngangsvilkår(vilkårene, behandlingReferanse);

        var avslåttTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(perioderMedAvslåtteInngangsvilkår, getKantIKantVurderer(behandlingReferanse));
        // Fjern perioder som overlapper med perioder som har avslåtte inngangsvilkår
        return aktuelleVilkårsperioder
            .stream()
            .filter(p -> avslåttTidslinje.getLocalDateIntervals().stream().anyMatch(di -> di.equals(new LocalDateInterval(p.getFomDato(), p.getTomDato()))))
            .collect(Collectors.toSet());
    }

    private Set<DatoIntervallEntitet> finnPerioderMedAvslåtteInngangsvilkår(Vilkårene vilkårene, BehandlingReferanse behandlingReferanse) {
        var kantIKantVurderer = getKantIKantVurderer(behandlingReferanse);
        return FinnPerioderMedAvslåtteInngangsvilkårForBeregning.finnPerioderMedAvslåtteInngangsvilkår(vilkårene, kantIKantVurderer);
    }

    private KantIKantVurderer getKantIKantVurderer(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType()).getKantIKantVurderer();
    }

}
