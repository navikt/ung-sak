package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnPerioderSomSkalFjernesIBeregning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.FinnPerioderMedAvslåtteInngangsvilkårForBeregning;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
public class OMPFinnPerioderSomSkalFjernesIBeregning implements FinnPerioderSomSkalFjernesIBeregning {
    private static final Set<VilkårType> VILKÅR_HVOR_AVSLAG_IKKE_SKAL_FJERNES = Set.of(
        VilkårType.OPPTJENINGSVILKÅRET,
        VilkårType.OPPTJENINGSPERIODEVILKÅR
    );

    private ÅrskvantumTjeneste årskvantumTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    public OMPFinnPerioderSomSkalFjernesIBeregning() {}

    @Inject
    public OMPFinnPerioderSomSkalFjernesIBeregning(
        ÅrskvantumTjeneste årskvantumTjeneste,
        @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste

    ) {
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public Set<DatoIntervallEntitet> finnPerioderSomSkalFjernes(Vilkårene vilkårene, BehandlingReferanse behandlingReferanse) {
        var uttaksplan = årskvantumTjeneste.hentFullUttaksplan(behandlingReferanse.getSaksnummer());
        var perioderMedAvslåtteUttaksvilkår = finnPerioderMedAvslåtteUttaksvilkår(uttaksplan);
        var perioderMedAvslåtteBeregningsvilkår = FinnPerioderMedAvslåtteInngangsvilkårForBeregning.finnPerioderMedAvslåtteInngangsvilkår(vilkårene, finnKantIKantVurderer(behandlingReferanse));
        // Fjern perioder som overlapper med perioder som har avslåtte inngangsvilkår eller er avslått i uttak
        return vilkårene
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow(() -> new IllegalStateException("Hadde ikke beregningsGrunnlagvilkår"))
            .getPerioder().stream()
            .filter(vilkårPeriode -> no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT.equals(vilkårPeriode.getUtfall()))
            .map(VilkårPeriode::getPeriode)
            .filter(periode ->
                perioderMedAvslåtteBeregningsvilkår.stream().anyMatch(periode::overlapper) || perioderMedAvslåtteUttaksvilkår.stream().anyMatch(periode::overlapper)
            )
            .collect(Collectors.toSet());
    }

    private KantIKantVurderer finnKantIKantVurderer(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType()).getKantIKantVurderer();
    }

    private static Set<DatoIntervallEntitet> finnPerioderMedAvslåtteUttaksvilkår(FullUttaksplan uttaksplan) {
        return uttaksplan.getAktiviteter().stream()
            .flatMap(aktivitet -> aktivitet.getUttaksperioder().stream())
            .filter(periode ->
                periode.getVurderteVilkår().getVilkår().values().stream().anyMatch(Utfall.AVSLÅTT::equals)
            )
            .map(periode -> {
                var fom = periode.getPeriode().getFom();
                var tom = periode.getPeriode().getTom();
                return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            })
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
