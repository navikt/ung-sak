package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnPerioderSomSkalFjernesIBeregning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.FinnPerioderMedAvslåtteInngangsvilkårForBeregning;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
public class PleiepengerFinnPerioderSomSkalFjernesIBeregning implements FinnPerioderSomSkalFjernesIBeregning {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    public PleiepengerFinnPerioderSomSkalFjernesIBeregning() {}

    @Inject
    public PleiepengerFinnPerioderSomSkalFjernesIBeregning(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public Set<DatoIntervallEntitet> finnPerioderSomSkalFjernes(Vilkårene vilkårene, BehandlingReferanse behandlingReferanse) {
        var perioderMedAvslåtteInngangsvilkår = finnPerioderMedAvslåtteInngangsvilkår(vilkårene, behandlingReferanse);
        // Fjern perioder som overlapper med perioder som har avslåtte inngangsvilkår
        return vilkårene
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow(() -> new IllegalStateException("Hadde ikke beregningsGrunnlagvilkår"))
            .getPerioder().stream()
            .filter(vilkårPeriode -> no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT.equals(vilkårPeriode.getUtfall()))
            .map(VilkårPeriode::getPeriode)
            .filter(periode ->
                perioderMedAvslåtteInngangsvilkår.stream().anyMatch(vp -> vp.getFomDato().isEqual(periode.getFomDato()))
            )
            .collect(Collectors.toSet());
    }

    private Set<DatoIntervallEntitet> finnPerioderMedAvslåtteInngangsvilkår(Vilkårene vilkårene, BehandlingReferanse behandlingReferanse) {
        var kantIKantVurderer = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType()).getKantIKantVurderer();
        return FinnPerioderMedAvslåtteInngangsvilkårForBeregning.finnPerioderMedAvslåtteInngangsvilkår(vilkårene, kantIKantVurderer);
    }

}