package no.nav.foreldrepenger.fagsak.hendelse;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public interface OppdaterFagsakStatus {
    void oppdaterFagsakNårBehandlingEndret(Behandling behandling);

    void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak);
}
