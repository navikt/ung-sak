package no.nav.k9.sak.vilkår;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;

public class VilkårPeriodeFilter {

    private final boolean skalMarkereForlengelser;
    private final BehandlingReferanse behandlingReferanse;
    private final FagsakRepository fagsakRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final ForlengelseTjeneste forlengelseTjeneste;
    private boolean skalIgnorereAvslåttePerioder;
    private boolean skalIgnorereAvslagPåKompletthet;
    private boolean skalIgnorerePerioderFraInfotrygd;
    private boolean skalIgnorereForlengelser;


    VilkårPeriodeFilter(boolean skalMarkereForlengelser, BehandlingReferanse behandlingReferanse, FagsakRepository fagsakRepository,
                        VilkårResultatRepository vilkårResultatRepository,
                        ForlengelseTjeneste forlengelseTjeneste) {
        this.skalMarkereForlengelser = skalMarkereForlengelser;
        this.behandlingReferanse = behandlingReferanse;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forlengelseTjeneste = forlengelseTjeneste;
    }


    public NavigableSet<PeriodeTilVurdering> utledPerioderTilVurdering(Collection<DatoIntervallEntitet> perioder, VilkårType vilkårType) {
        var sakInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(behandlingReferanse.getFagsakId());
        var behandlingId = behandlingReferanse.getBehandlingId();
        var vilkår = hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));

        var filterPerioder = perioder.stream()
            .map(PeriodeTilVurdering::new)
            .collect(Collectors.toCollection(TreeSet::new));

        if (vilkår.isPresent() && skalIgnorereAvslåttePerioder) {
            var avslåttePerioder = vilkår.get()
                .getPerioder()
                .stream()
                .filter(it -> skalFiltreresBort(it, skalIgnorereAvslagPåKompletthet))
                .map(VilkårPeriode::getPeriode).toList();
            avslåttePerioder.forEach(p -> filterPerioder.removeIf(fp -> fp.getPeriode().equals(p)));
        }
        if (vilkår.isPresent() && skalIgnorerePerioderFraInfotrygd) {
            var periodeFraInfotrygd = vilkår.get()
                .getPerioder()
                .stream()
                .map(VilkårPeriode::getPeriode)
                .filter(periode -> sakInfotrygdMigreringer.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt).anyMatch(periode::inkluderer))
                .toList();
            periodeFraInfotrygd.forEach(p -> filterPerioder.removeIf(fp -> fp.getPeriode().equals(p)));
        }
        if (skalIgnorereForlengelser) {
            var forlengelser = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(behandlingReferanse, filterPerioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new)), vilkårType);
            forlengelser.forEach(p -> filterPerioder.removeIf(fp -> fp.getPeriode().equals(p)));
        } else if (skalMarkereForlengelser) {
            var forlengelser = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(behandlingReferanse, filterPerioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new)), vilkårType);
            forlengelser.forEach(p -> filterPerioder.forEach(fp -> fp.setErForlengelse(fp.getPeriode().equals(p))));
        }
        return Collections.unmodifiableNavigableSet(filterPerioder);
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

    public VilkårPeriodeFilter ignorerAvslåttePerioder() {
        this.skalIgnorereAvslåttePerioder = true;
        return this;
    }

    public VilkårPeriodeFilter ignorerAvslagPåKompletthet() {
        this.skalIgnorereAvslagPåKompletthet = true;
        return this;
    }

    public VilkårPeriodeFilter ignorerPerioderFraInfotrygd() {
        this.skalIgnorerePerioderFraInfotrygd = true;
        return this;
    }

}
