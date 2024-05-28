package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnPerioderSomSkalFjernesIBeregning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
public class PSBFinnPerioderSomSkalFjernesIBeregning implements FinnPerioderSomSkalFjernesIBeregning {
    private static final Set<VilkårType> VILKÅR_HVOR_AVSLAG_IKKE_SKAL_FJERNES = Set.of(
        VilkårType.OPPTJENINGSVILKÅRET,
        VilkårType.OPPTJENINGSPERIODEVILKÅR
    );

    public PSBFinnPerioderSomSkalFjernesIBeregning() {}

    @Override
    public Set<DatoIntervallEntitet> finnPerioderMedAvslåtteVilkår(VilkårBuilder vilkårBuilder, Vilkårene vilkårene, BehandlingReferanse behandlingReferanse) {
        var perioderMedAvslåtteBeregningsvilkår = finnPerioderMedAvslåtteBeregningsvilkår(vilkårene);
        // Fjern perioder som overlapper med perioder som har avslåtte vilkår i enten uttak eller beregning
        return vilkårene
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow(() -> new IllegalStateException("Hadde ikke beregingsGrunnlagvilkår"))
            .getPerioder().stream()
            .filter(vilkårPeriode -> no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT.equals(vilkårPeriode.getUtfall()))
            .map(VilkårPeriode::getPeriode)
            .filter(periode ->
                perioderMedAvslåtteBeregningsvilkår.stream().anyMatch(periode::overlapper)
            )
            .collect(Collectors.toSet());
    }

    private static Set<DatoIntervallEntitet> finnPerioderMedAvslåtteBeregningsvilkår(Vilkårene vilkårene) {
        return vilkårene.getVilkårene().stream()
            .filter(v -> !VILKÅR_HVOR_AVSLAG_IKKE_SKAL_FJERNES.contains(v.getVilkårType()))
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> no.nav.k9.kodeverk.vilkår.Utfall.IKKE_OPPFYLT.equals(p.getUtfall()))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());
    }
}
