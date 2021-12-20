package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelOrkestrerer;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
public class InngangsvilkårFellesTjeneste {
    private RegelOrkestrerer regelOrkestrerer;
    private BehandlingRepository behandlingRepository;
    private Instance<ForlengelseTjeneste> forlengelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private Boolean enableForlengelse;

    InngangsvilkårFellesTjeneste() {
        // CDI
    }

    @Inject
    public InngangsvilkårFellesTjeneste(RegelOrkestrerer regelOrkestrerer,
                                        BehandlingRepository behandlingRepository,
                                        @Any Instance<ForlengelseTjeneste> forlengelseTjeneste,
                                        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                        @KonfigVerdi(value = "forlengelse.enablet", defaultVerdi = "false") Boolean enableForlengelse) {
        this.regelOrkestrerer = regelOrkestrerer;
        this.behandlingRepository = behandlingRepository;
        this.forlengelseTjeneste = forlengelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.enableForlengelse = enableForlengelse;
    }

    RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> intervaller) {
        return regelOrkestrerer.vurderInngangsvilkår(vilkårHåndtertAvSteg, ref, intervaller);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var tjeneste = getPerioderTilVurderingTjeneste(behandling);
        var perioderTilVurdering = tjeneste.utled(behandlingId, vilkårType);

        if (enableForlengelse && behandling.getOriginalBehandlingId().isPresent()) {
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

    public Boolean getEnableForlengelse() {
        return enableForlengelse;
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
