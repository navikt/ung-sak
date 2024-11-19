package no.nav.ung.sak.vilkår;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ForlengelseTjeneste;

public class VilkårPeriodeFilter {

    private final BehandlingReferanse behandlingReferanse;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final ForlengelseTjeneste forlengelseTjeneste;
    private final EndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder;
    private final List<Avslagsårsak> avslagsårsakerSomIkkeFiltreresBort = new ArrayList<>();

    private boolean skalIgnorereAvslåttePerioder;
    private boolean skalIgnorereForlengelser;
    private boolean skalMarkereEndringIUttak;


    VilkårPeriodeFilter(BehandlingReferanse behandlingReferanse,
                        VilkårResultatRepository vilkårResultatRepository,
                        ForlengelseTjeneste forlengelseTjeneste,
                        EndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder) {
        this.behandlingReferanse = behandlingReferanse;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forlengelseTjeneste = forlengelseTjeneste;
        this.endringIUttakPeriodeUtleder = endringIUttakPeriodeUtleder;
    }


    public NavigableSet<PeriodeTilVurdering> filtrerPerioder(Collection<DatoIntervallEntitet> perioder, VilkårType vilkårType) {
        var behandlingId = behandlingReferanse.getBehandlingId();
        var vilkår = hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        var filtrertePerioder = filtrer(vilkårType, vilkår, perioder);
        markerForlengelser(vilkårType, filtrertePerioder);
        if (skalMarkereEndringIUttak) {
            markerEndringIUttak(filtrertePerioder);
        }
        return Collections.unmodifiableNavigableSet(filtrertePerioder);
    }

    private TreeSet<PeriodeTilVurdering> filtrer(VilkårType vilkårType,
                                                 Optional<Vilkår> vilkår,
                                                 Collection<DatoIntervallEntitet> inputPerioder) {
        var filterPerioder = new HashSet<>(inputPerioder);
        if (skalIgnorereAvslåttePerioder) {
            var avslåttePerioderUtenInkludertÅrsak = filtrerVilkårsperiode(vilkår, this::avslåttUtenInkludertÅrsak);
            filtrerBort(filterPerioder, avslåttePerioderUtenInkludertÅrsak);
        }
        if (skalIgnorereForlengelser) {
            var forlengelser = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(behandlingReferanse, new TreeSet<>(filterPerioder), vilkårType);
            filtrerBort(filterPerioder, forlengelser);
        }
        return filterPerioder.stream()
            .map(PeriodeTilVurdering::new)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private void markerForlengelser(VilkårType vilkårType, TreeSet<PeriodeTilVurdering> filterPerioder) {
        var forlengelser = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(behandlingReferanse, filterPerioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new)), vilkårType);
        filterPerioder.forEach(fp -> fp.setErForlengelse(forlengelser.stream().anyMatch(p -> fp.getPeriode().equals(p))));
    }

    private void markerEndringIUttak(TreeSet<PeriodeTilVurdering> filterPerioder) {
        var endringerIUttak = endringIUttakPeriodeUtleder.utled(behandlingReferanse);
        filterPerioder.forEach(p -> p.setErEndringIUttak(endringerIUttak.contains(p.getPeriode())));
    }


    private Set<DatoIntervallEntitet> filtrerVilkårsperiode(Optional<Vilkår> vilkår, Predicate<VilkårPeriode> predicate) {
        return vilkår.map(v ->
                v.getPerioder()
                    .stream()
                    .filter(predicate)
                    .map(VilkårPeriode::getPeriode)
                    .collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
    }

    private void filtrerBort(Collection<DatoIntervallEntitet> perioder, Collection<DatoIntervallEntitet> fjernPerioder) {
        fjernPerioder.forEach(p -> perioder.removeIf(p::equals));
    }


    private boolean avslåttUtenInkludertÅrsak(VilkårPeriode it) {
        return Utfall.IKKE_OPPFYLT.equals(it.getUtfall()) && !avslagsårsakerSomIkkeFiltreresBort.contains(it.getAvslagsårsak());
    }

    public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId);
    }


    public VilkårPeriodeFilter ignorerForlengelseperioder() {
        this.skalIgnorereForlengelser = true;
        return this;
    }

    public VilkårPeriodeFilter ignorerAvslåttePerioder() {
        this.skalIgnorereAvslåttePerioder = true;
        return this;
    }

    public VilkårPeriodeFilter ignorerAvslåtteUnntattForLavtBeregningsgrunnlag() {
        this.skalIgnorereAvslåttePerioder = true;
        this.avslagsårsakerSomIkkeFiltreresBort.add(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG);
        this.avslagsårsakerSomIkkeFiltreresBort.add(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG_8_47);
        return this;
    }

    public VilkårPeriodeFilter ignorerAvslåttePerioderUnntattKompletthet() {
        this.skalIgnorereAvslåttePerioder = true;
        avslagsårsakerSomIkkeFiltreresBort.add(Avslagsårsak.MANGLENDE_INNTEKTSGRUNNLAG);
        return this;
    }

    public VilkårPeriodeFilter markerEndringIUttak() {
        this.skalMarkereEndringIUttak = true;
        return this;
    }


}
