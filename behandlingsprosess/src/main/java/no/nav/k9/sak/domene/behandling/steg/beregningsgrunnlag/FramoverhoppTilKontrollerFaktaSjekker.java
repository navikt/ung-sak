package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@ApplicationScoped
public class FramoverhoppTilKontrollerFaktaSjekker {

    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;


    @Inject
    public FramoverhoppTilKontrollerFaktaSjekker(VilkårResultatRepository vilkårResultatRepository,
                                                 VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
    }

    public FramoverhoppTilKontrollerFaktaSjekker() {
        // CDI Proxy
    }

    public NavigableSet<PeriodeTilVurdering> finnPerioderForFramoverhoppTilKontrollerFakta(BehandlingReferanse ref,
                                                                                  NavigableSet<PeriodeTilVurdering> allePerioder,
                                                                                  Set<PeriodeTilVurdering> forlengelseperioderBeregning) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåttePerioder();
        var oppfylteBeregningsperioderForrigeBehandling = finnOppfylteVilkårsperioderForrigeBehandling(ref);
        return periodeFilter.filtrerPerioder(allePerioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet()), VilkårType.OPPTJENINGSVILKÅRET).stream()
            .filter(PeriodeTilVurdering::erForlengelse)
            .filter(periode -> forlengelseperioderBeregning.stream().noneMatch(it -> it.getPeriode().equals(periode.getPeriode())))
            .filter(periode -> oppfylteBeregningsperioderForrigeBehandling.stream().anyMatch(it -> it.equals(periode.getPeriode())))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<DatoIntervallEntitet> finnOppfylteVilkårsperioderForrigeBehandling(BehandlingReferanse ref) {
        return vilkårResultatRepository.hentHvisEksisterer(ref.getOriginalBehandlingId().orElseThrow()).orElseThrow()
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());
    }


}
