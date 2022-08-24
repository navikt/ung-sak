package no.nav.k9.sak.vilkår;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;

public class VilkårPeriodeFilter {

    private final BehandlingReferanse behandlingReferanse;
    private final FagsakRepository fagsakRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final ForlengelseTjeneste forlengelseTjeneste;
    private final EndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder;
    private boolean skalIgnorereAvslåttePerioder;
    private boolean skalIgnorereAvslagPåKompletthet;
    private boolean skalIgnorerePerioderFraInfotrygd;
    private boolean skalIgnorereForlengelser;


    VilkårPeriodeFilter(BehandlingReferanse behandlingReferanse,
                        FagsakRepository fagsakRepository,
                        VilkårResultatRepository vilkårResultatRepository,
                        ForlengelseTjeneste forlengelseTjeneste,
                        EndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder) {
        this.behandlingReferanse = behandlingReferanse;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forlengelseTjeneste = forlengelseTjeneste;
        this.endringIUttakPeriodeUtleder = endringIUttakPeriodeUtleder;
    }


    public NavigableSet<PeriodeTilVurdering> filtrerPerioder(Collection<DatoIntervallEntitet> perioder, VilkårType vilkårType) {
        var sakInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(behandlingReferanse.getFagsakId());
        var behandlingId = behandlingReferanse.getBehandlingId();
        var vilkår = hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        var filtrertePerioder = filtrer(vilkårType, sakInfotrygdMigreringer, vilkår, perioder);
        markerForlengelser(vilkårType, filtrertePerioder);
        markerEndringIUttak(filtrertePerioder);
        return Collections.unmodifiableNavigableSet(filtrertePerioder);
    }

    private TreeSet<PeriodeTilVurdering> filtrer(VilkårType vilkårType,
                                                 List<SakInfotrygdMigrering> sakInfotrygdMigreringer,
                                                 Optional<Vilkår> vilkår,
                                                 Collection<DatoIntervallEntitet> filterPerioder) {
        if (skalIgnorereAvslåttePerioder) {
            var avslåttePerioder = filtrerVilkårsperiode(vilkår, avslåttPeriodePredicate());
            filtrerBort(filterPerioder, avslåttePerioder);
        }
        if (skalIgnorerePerioderFraInfotrygd) {
            var periodeFraInfotrygd = filtrerVilkårsperiode(vilkår, periodeFraInfotrygdPredicate(sakInfotrygdMigreringer));
            filtrerBort(filterPerioder, periodeFraInfotrygd);
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

    private Predicate<VilkårPeriode> periodeFraInfotrygdPredicate(List<SakInfotrygdMigrering> sakInfotrygdMigreringer) {
        return periode -> sakInfotrygdMigreringer.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt).anyMatch(stp -> periode.getPeriode().inkluderer(stp));
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

    private Predicate<VilkårPeriode> avslåttPeriodePredicate() {
        return it -> skalFiltreresBort(it, skalIgnorereAvslagPåKompletthet);
    }

    private void filtrerBort(Collection<DatoIntervallEntitet> perioder, Collection<DatoIntervallEntitet> fjernPerioder) {
        fjernPerioder.forEach(p -> perioder.removeIf(p::equals));
    }


    private boolean skalFiltreresBort(VilkårPeriode it, boolean skalIgnoreAvslagPåKompletthet) {
        return Utfall.IKKE_OPPFYLT.equals(it.getUtfall()) && (skalIgnoreAvslagPåKompletthet || !Avslagsårsak.MANGLENDE_INNTEKTSGRUNNLAG.equals(it.getAvslagsårsak()));
    }

    public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId);
    }


    public VilkårPeriodeFilter ignorerForlengelseperioder() {
        this.skalIgnorereForlengelser = true;
        return this;
    }

    public VilkårPeriodeFilter ignorerAvslåttePerioderInkludertKompletthet() {
        this.skalIgnorereAvslåttePerioder = true;
        this.skalIgnorereAvslagPåKompletthet = true;
        return this;
    }

    public VilkårPeriodeFilter ignorerAvslåttePerioderUnntattKompletthet() {
        this.skalIgnorereAvslåttePerioder = true;
        this.skalIgnorereAvslagPåKompletthet = false;
        return this;
    }

    public VilkårPeriodeFilter ignorerPerioderFraInfotrygd() {
        this.skalIgnorerePerioderFraInfotrygd = true;
        return this;
    }

}
