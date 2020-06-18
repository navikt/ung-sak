package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import java.util.NavigableSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelOrkestrerer;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
public class InngangsvilkårFellesTjeneste {
    private RegelOrkestrerer regelOrkestrerer;
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;

    InngangsvilkårFellesTjeneste() {
        // CDI
    }

    @Inject
    public InngangsvilkårFellesTjeneste(RegelOrkestrerer regelOrkestrerer,
                                        BehandlingRepository behandlingRepository, @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste) {
        this.regelOrkestrerer = regelOrkestrerer;
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> intervaller) {
        return regelOrkestrerer.vurderInngangsvilkår(vilkårHåndtertAvSteg, ref, intervaller);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(perioderTilVurderingTjeneste, behandling.getFagsakYtelseType()).orElseThrow();
        return tjeneste.utled(behandlingId, vilkårType);
    }
}
