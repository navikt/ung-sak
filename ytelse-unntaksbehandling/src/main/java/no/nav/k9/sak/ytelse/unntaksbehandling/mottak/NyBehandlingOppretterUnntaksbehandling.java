package no.nav.k9.sak.ytelse.unntaksbehandling.mottak;

import static no.nav.k9.kodeverk.behandling.BehandlingType.UNNTAKSBEHANDLING;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.revurdering.UnntaksbehandlingOppretter;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.NyBehandlingOppretter;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@FagsakYtelseTypeRef
@BehandlingTypeRef(UNNTAKSBEHANDLING)
@ApplicationScoped
public class NyBehandlingOppretterUnntaksbehandling implements NyBehandlingOppretter {

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private UnntaksbehandlingOppretter unntaksbehandlingOppretter;

    @Inject
    public NyBehandlingOppretterUnntaksbehandling(BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                                  @FagsakYtelseTypeRef @BehandlingTypeRef(UNNTAKSBEHANDLING) UnntaksbehandlingOppretter unntaksbehandlingOppretter) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.unntaksbehandlingOppretter = unntaksbehandlingOppretter;
    }

    @Override
    public Behandling opprettNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        Behandling revurdering = unntaksbehandlingOppretter.opprettNyBehandling(origBehandling.getFagsak(), origBehandling, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(origBehandling.getFagsak()));
        return revurdering;
    }
}
