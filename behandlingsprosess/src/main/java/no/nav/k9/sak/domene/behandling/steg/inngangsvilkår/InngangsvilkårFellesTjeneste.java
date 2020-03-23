package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkaar.RegelOrkestrerer;
import no.nav.k9.sak.inngangsvilkaar.RegelResultat;
import no.nav.k9.sak.inngangsvilkaar.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class InngangsvilkårFellesTjeneste  {
    private RegelOrkestrerer regelOrkestrerer;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    InngangsvilkårFellesTjeneste() {
        // CDI
    }

    @Inject
    public InngangsvilkårFellesTjeneste(RegelOrkestrerer regelOrkestrerer, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.regelOrkestrerer = regelOrkestrerer;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, BehandlingReferanse ref, List<DatoIntervallEntitet> intervaller) {
        return regelOrkestrerer.vurderInngangsvilkår(vilkårHåndtertAvSteg, ref, intervaller);
    }

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
    }

    public Set<DatoIntervallEntitet> utledPerioderTilVurdering(Long behandlingId, VilkårType vilkårType) {
        return perioderTilVurderingTjeneste.utled(behandlingId, vilkårType);
    }
}
