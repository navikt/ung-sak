package no.nav.ung.sak.vilkår;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

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

public class VilkårPeriodeFilter {

    private final BehandlingReferanse behandlingReferanse;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final List<Avslagsårsak> avslagsårsakerSomIkkeFiltreresBort = new ArrayList<>();

    private boolean skalIgnorereAvslåttePerioder;
    private boolean skalIgnorereIkkeRelevantePerioder;


    VilkårPeriodeFilter(BehandlingReferanse behandlingReferanse,
                        VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingReferanse = behandlingReferanse;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public NavigableSet<PeriodeTilVurdering> filtrerPerioder(Collection<DatoIntervallEntitet> perioder, VilkårType vilkårType) {
        var behandlingId = behandlingReferanse.getBehandlingId();
        var vilkår = hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        var filtrertePerioder = filtrer(vilkår, perioder);
        return Collections.unmodifiableNavigableSet(filtrertePerioder);
    }


    /**
     * Ignorerer perioder som er avslått, uavhengig av årsak.
     *
     * @return this
     */
    public VilkårPeriodeFilter ignorerAvslåttePerioder() {
        this.skalIgnorereAvslåttePerioder = true;
        return this;
    }

    /**
     * Ignorerer perioder som er vurdert som ikke relevante.
     * <p>
     * Dette kan være perioder som er vurdert som ikke relevante i tidligere behandlinger, eller perioder som er
     * vurdert som ikke relevante i denne behandlingen på grunn av avslåtte avhengige vilkår.
     * Operasjoner som rydder eller gjenoppretter vilkår vil typisk ønske å inkludere perioder som er markert som ikke-relevant for å kunne vurdere denne statusen på nytt i tilfelle der status på avhengige vilkår endres.
     * Operasjoner som som utgjør hele eller deler av vilkårsvurderinger vil typisk ønske å ignorere perioder som er vurdert som ikke relevante.
     *
     * @return this
     */
    public VilkårPeriodeFilter ignorerIkkeRelevantePerioder() {
        this.skalIgnorereIkkeRelevantePerioder = true;
        return this;
    }

    private Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId);
    }

    private TreeSet<PeriodeTilVurdering> filtrer(Optional<Vilkår> vilkår,
                                                 Collection<DatoIntervallEntitet> inputPerioder) {
        var filterPerioder = new HashSet<>(inputPerioder);
        if (skalIgnorereAvslåttePerioder) {
            var avslåttePerioderUtenInkludertÅrsak = filtrerVilkårsperiode(vilkår, this::avslåttUtenInkludertÅrsak);
            filtrerBort(filterPerioder, avslåttePerioderUtenInkludertÅrsak);
        }
        if (skalIgnorereIkkeRelevantePerioder) {
            var ikkeRelevantePerioder = filtrerVilkårsperiode(vilkår, this::ikkeRelevant);
            filtrerBort(filterPerioder, ikkeRelevantePerioder);
        }
        return filterPerioder.stream()
            .map(PeriodeTilVurdering::new)
            .collect(Collectors.toCollection(TreeSet::new));
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


    private boolean ikkeRelevant(VilkårPeriode it) {
        return Utfall.IKKE_RELEVANT.equals(it.getUtfall());
    }


}
