package no.nav.k9.sak.mottak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class NyBehandlingOppretterRevurdering implements NyBehandlingOppretter {

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Inject
    public NyBehandlingOppretterRevurdering(BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    public Behandling opprettNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, origBehandling.getFagsakYtelseType()).orElseThrow();
        Behandling revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(origBehandling.getFagsak()));
        return revurdering;
    }
}
