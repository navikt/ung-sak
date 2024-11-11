package no.nav.ung.sak.behandling.revurdering;


import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

public interface UnntaksbehandlingOppretter {
    Behandling opprettNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet);
    Boolean kanNyBehandlingOpprettes(Fagsak fagsak);
}
