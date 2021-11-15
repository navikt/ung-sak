package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.NavigableSet;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@Dependent
public class BeregningsgrunnlagVilkårTjeneste {

    private VilkårTjeneste vilkårTjeneste;
    private final VilkårType vilkårType = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;

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

    public void settVilkårutfallTilIkkeVurdert(Long behandlingId, NavigableSet<DatoIntervallEntitet> vilkårsPerioder) {
        vilkårTjeneste.settVilkårutfallTilIkkeVurdert(behandlingId, vilkårType, vilkårsPerioder);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, boolean skalIgnorereAvslåttePerioder) {
        return vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType, skalIgnorereAvslåttePerioder);
    }

}
