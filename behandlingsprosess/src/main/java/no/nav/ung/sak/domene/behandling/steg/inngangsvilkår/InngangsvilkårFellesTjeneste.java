package no.nav.ung.sak.domene.behandling.steg.inngangsvilkår;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.inngangsvilkår.RegelOrkestrerer;
import no.nav.ung.sak.inngangsvilkår.RegelResultat;
import no.nav.ung.sak.perioder.ForlengelseTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
public class InngangsvilkårFellesTjeneste {
    private RegelOrkestrerer regelOrkestrerer;
    private BehandlingRepository behandlingRepository;
    private Instance<ForlengelseTjeneste> forlengelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;

    InngangsvilkårFellesTjeneste() {
        // CDI
    }

    @Inject
    public InngangsvilkårFellesTjeneste(RegelOrkestrerer regelOrkestrerer,
                                        BehandlingRepository behandlingRepository,
                                        @Any Instance<ForlengelseTjeneste> forlengelseTjeneste,
                                        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste) {
        this.regelOrkestrerer = regelOrkestrerer;
        this.behandlingRepository = behandlingRepository;
        this.forlengelseTjeneste = forlengelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> intervaller) {
        return regelOrkestrerer.vurderInngangsvilkår(vilkårHåndtertAvSteg, ref, intervaller);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var tjeneste = getPerioderTilVurderingTjeneste(behandling);
        var perioderTilVurdering = tjeneste.utled(behandlingId, vilkårType);

        if (behandling.getOriginalBehandlingId().isPresent()) {
            // Trekk fra periodene som er forlengelse
            var forlengelseTjeneste = ForlengelseTjeneste.finnTjeneste(this.forlengelseTjeneste, behandling.getFagsakYtelseType(), behandling.getType());

            var forlengelsePerioder = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(BehandlingReferanse.fra(behandling), perioderTilVurdering, vilkårType);

            perioderTilVurdering = perioderTilVurdering.stream()
                .filter(it -> !forlengelsePerioder.contains(it))
                .collect(Collectors.toCollection(TreeSet::new));
        }

        return perioderTilVurdering;
    }

    public NavigableSet<DatoIntervallEntitet> utledForlengelserTilVurdering(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var tjeneste = getPerioderTilVurderingTjeneste(behandling);
        var perioderTilVurdering = tjeneste.utled(behandlingId, vilkårType);
        var forlengelseTjeneste = ForlengelseTjeneste.finnTjeneste(this.forlengelseTjeneste, behandling.getFagsakYtelseType(), behandling.getType());

        return forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(BehandlingReferanse.fra(behandling), perioderTilVurdering, vilkårType);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
