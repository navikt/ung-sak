package no.nav.k9.sak.behandling.revurdering;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class DefaultUnntaksbehandlingOppretterTjeneste implements UnntaksbehandlingOppretterTjeneste {


    @Override
    public Behandling opprettNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        throw new IllegalArgumentException("Ikke tillatt å opprette unntaksbehandling her");
    }

    @Override
    public Boolean kanNyBehandlingOpprettes(Fagsak fagsak) {
        throw new IllegalArgumentException("Ikke tillatt å opprette unntaksbehandling her");
    }


}
