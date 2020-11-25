package no.nav.k9.sak.behandling.revurdering;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public interface RevurderingTjeneste {
    Behandling opprettAutomatiskNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet finnBehandlendeEnhetFor);

    Behandling opprettManueltNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType behandlingÅrsakType, OrganisasjonsEnhet enhet);

    void kopierAlleGrunnlagFraTidligereBehandling(Behandling behandlingMedSøknad, Behandling behandling);

    Boolean kanNyBehandlingOpprettes(Fagsak fagsak);

}
