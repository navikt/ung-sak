package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.perioder.PerioderTilVurderingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@ApplicationScoped
public class InngangsvilkårFellesTjeneste  {
    private RegelOrkestrerer regelOrkestrerer;
    private PerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    InngangsvilkårFellesTjeneste() {
        // CDI
    }

    @Inject
    public InngangsvilkårFellesTjeneste(RegelOrkestrerer regelOrkestrerer, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste, PerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.regelOrkestrerer = regelOrkestrerer;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, Behandling behandling, BehandlingReferanse ref, List<DatoIntervallEntitet> intervaller) {
        return regelOrkestrerer.vurderInngangsvilkår(vilkårHåndtertAvSteg, behandling, ref, intervaller);
    }

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
    }

    public Set<DatoIntervallEntitet> utledPerioderTilVurdering(Long behandlingId) {
        return perioderTilVurderingTjeneste.utled(behandlingId);
    }
}
