package no.nav.k9.sak.behandling.revurdering;


import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public interface UnntaksbehandlingOppretter {
    Behandling opprettNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet);
    Boolean kanNyBehandlingOpprettes(Fagsak fagsak);
}
