package no.nav.ung.sak.mottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class NyBehandlingOppretterRevurdering implements NyBehandlingOppretter {

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private Instance<RevurderingTjeneste> revurderingTjenester;

    @Inject
    public NyBehandlingOppretterRevurdering(BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                            @Any Instance<RevurderingTjeneste> revurderingTjenester) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingTjenester = revurderingTjenester;
    }

    @Override
    public Behandling opprettNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(revurderingTjenester, origBehandling.getFagsakYtelseType()).orElseThrow();
        Behandling revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(origBehandling.getFagsak()));
        return revurdering;
    }
}
