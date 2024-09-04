package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilter;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@Dependent
public class BeregningsgrunnlagVilkårTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeregningsgrunnlagVilkårTjeneste.class);

    private final VilkårType vilkårType = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
    private VilkårTjeneste vilkårTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(VilkårTjeneste vilkårTjeneste,
                                            VilkårResultatRepository vilkårResultatRepository,
                                            VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
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

    public void gjenopprettVilkårsutfallVedBehov(BehandlingReferanse referanse, Collection<DatoIntervallEntitet> perioderTilVurdering) {
        vilkårTjeneste.gjenopprettVilkårsutfallForPerioderSomIkkeVurderes(referanse, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, perioderTilVurdering);
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
        var vilkårPeriodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        if (skalIgnorereAvslåttePerioder) {
            vilkårPeriodeFilter.ignorerAvslåttePerioder();
        }

        var perioder = vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType);

        return vilkårPeriodeFilter.filtrerPerioder(perioder, vilkårType).stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new));
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderForKompletthet(BehandlingReferanse ref, boolean skalIgnorereAvslåttePerioder, boolean skalIgnorereAvslagPåKompletthet, boolean skalIgnorerePerioderFraInfotrygd) {
        var perioderTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType);
        var vilkårPeriodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        if (skalIgnorereAvslåttePerioder && skalIgnorereAvslagPåKompletthet) {
            vilkårPeriodeFilter.ignorerAvslåttePerioder();
        }
        if (skalIgnorereAvslåttePerioder && !skalIgnorereAvslagPåKompletthet) {
            vilkårPeriodeFilter.ignorerAvslåttePerioderUnntattKompletthet();
        }
        if (skalIgnorerePerioderFraInfotrygd) {
            vilkårPeriodeFilter.ignorerPerioderFraInfotrygd();
        }

        vilkårPeriodeFilter.ignorerForlengelseperioder();

        return vilkårPeriodeFilter.filtrerPerioder(perioderTilVurdering, vilkårType).stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new));
    }

    public NavigableSet<PeriodeTilVurdering> utledDetaljertPerioderTilVurdering(BehandlingReferanse ref, VilkårPeriodeFilter vilkårPeriodeFilter) {
        var allePerioder = vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType);
        return vilkårPeriodeFilter.filtrerPerioder(allePerioder, vilkårType);
    }

    public NavigableSet<PeriodeTilVurdering> utledDetaljertPerioderTilVurdering(BehandlingReferanse ref) {
        var allePerioder = vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType);
        return vilkårPeriodeFilterProvider.getFilter(ref).filtrerPerioder(allePerioder, vilkårType);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, VilkårPeriodeFilter vilkårPeriodeFilter) {
        var allePerioder = vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType);
        return vilkårPeriodeFilter.filtrerPerioder(allePerioder, vilkårType)
            .stream()
            .map(PeriodeTilVurdering::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref) {
        return vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType);
    }

}
