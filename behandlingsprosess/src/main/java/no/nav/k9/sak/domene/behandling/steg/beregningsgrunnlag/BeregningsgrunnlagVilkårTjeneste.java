package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilter;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@Dependent
public class BeregningsgrunnlagVilkårTjeneste {

    private final VilkårType vilkårType = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
    private VilkårTjeneste vilkårTjeneste;

    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(VilkårTjeneste vilkårTjeneste) {
        this.vilkårTjeneste = vilkårTjeneste;
    }

    public void lagreAvslåttVilkårresultat(BehandlingskontrollKontekst kontekst,
                                           DatoIntervallEntitet vilkårsPeriode,
                                           Avslagsårsak avslagsårsak) {
        vilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårType, vilkårsPeriode, null, avslagsårsak);
    }

    public void lagreAvslåttVilkårresultat(BehandlingskontrollKontekst kontekst,
                                           DatoIntervallEntitet vilkårsPeriode,
                                           String begrunnelse,
                                           Avslagsårsak avslagsårsak) {
        vilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårType, vilkårsPeriode, begrunnelse, avslagsårsak);
    }

    public void lagreVilkårresultat(BehandlingskontrollKontekst kontekst,
                                    DatoIntervallEntitet vilkårsPeriode, Avslagsårsak avslagsårsak) {
        vilkårTjeneste.lagreVilkårresultat(kontekst, vilkårType, vilkårsPeriode, avslagsårsak);
    }

    public void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet vilkårsPeriode) {
        vilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, vilkårType, vilkårsPeriode);
    }

    public void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst, NavigableSet<DatoIntervallEntitet> vilkårsPerioder) {
        vilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, vilkårType, vilkårsPerioder);
    }

    public void settVilkårutfallTilIkkeVurdertHvisTidligereAvslagPåKompletthet(Long behandlingId, NavigableSet<DatoIntervallEntitet> vilkårsPerioder) {
        var vilkåret = vilkårTjeneste.hentVilkårResultat(behandlingId).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .orElseThrow();

        var perioderSomSkalTilbakestilles = vilkåret.getPerioder()
            .stream()
            .filter(it -> vilkårsPerioder.stream()
                .anyMatch(at -> Objects.equals(it.getPeriode(), at) && Avslagsårsak.MANGLENDE_INNTEKTSGRUNNLAG.equals(it.getAvslagsårsak())))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));

        vilkårTjeneste.settVilkårutfallTilIkkeVurdert(behandlingId, vilkårType, perioderSomSkalTilbakestilles);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, boolean skalIgnorereAvslåttePerioder) {
        return vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType, skalIgnorereAvslåttePerioder, true, false);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, boolean skalIgnorereAvslåttePerioder, boolean skalIgnoreAvslagPåKompletthet, boolean skalIgnorerePerioderFraInfotrygd) {
        return vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType, skalIgnorereAvslåttePerioder, skalIgnoreAvslagPåKompletthet, skalIgnorerePerioderFraInfotrygd);
    }

    public NavigableSet<PeriodeTilVurdering> utledPerioderTilVurdering(BehandlingReferanse ref, VilkårPeriodeFilter vilkårPeriodeFilter) {
        return vilkårPeriodeFilter.utledPerioderTilVurdering(vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType), vilkårType);
    }

}
