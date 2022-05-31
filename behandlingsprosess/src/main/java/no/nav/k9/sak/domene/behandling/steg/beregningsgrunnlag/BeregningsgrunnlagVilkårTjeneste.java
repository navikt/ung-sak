package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilter;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@Dependent
public class BeregningsgrunnlagVilkårTjeneste {

    private final VilkårType vilkårType = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
    private VilkårTjeneste vilkårTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(VilkårTjeneste vilkårTjeneste,
                                            VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
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

    public void kopierVilkårresultatVedForlengelse(BehandlingskontrollKontekst kontekst,
                                                   Long originalBehandlingId,
                                                   Set<PeriodeTilVurdering> forlengelseperioder) {
        if (forlengelseperioder.stream().anyMatch(p -> !p.erForlengelse())) {
            throw new IllegalStateException("Kan kun kopiere resultat ved forlengelse");
        }
        var originalVilkårResultat = vilkårTjeneste.hentVilkårResultat(kontekst.getBehandlingId());
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(originalVilkårResultat);
        var vedtattUtfallPåVilkåret = vilkårTjeneste.hentHvisEksisterer(originalBehandlingId)
            .orElseThrow()
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .orElseThrow();

        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        for (var periode : forlengelseperioder) {
            var eksisteredeVurdering = vedtattUtfallPåVilkåret.finnPeriodeForSkjæringstidspunkt(periode.getPeriode().getFomDato());
            var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(periode.getPeriode())
                .forlengelseAv(eksisteredeVurdering);
            vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        }

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());

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

    public NavigableSet<PeriodeTilVurdering> utledDetaljertPerioderTilVurdering(BehandlingReferanse ref, VilkårPeriodeFilter vilkårPeriodeFilter) {
        var allePerioder = vilkårTjeneste.utledPerioderTilVurderingUfiltrert(ref, vilkårType);
        return vilkårPeriodeFilter.utledPerioderTilVurdering(allePerioder, vilkårType);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, VilkårPeriodeFilter vilkårPeriodeFilter) {
        var allePerioder = vilkårTjeneste.utledPerioderTilVurderingUfiltrert(ref, vilkårType);
        return vilkårPeriodeFilter.utledPerioderTilVurdering(allePerioder, vilkårType)
            .stream()
            .map(PeriodeTilVurdering::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref) {
        return vilkårTjeneste.utledPerioderTilVurderingUfiltrert(ref, vilkårType);
    }

}
