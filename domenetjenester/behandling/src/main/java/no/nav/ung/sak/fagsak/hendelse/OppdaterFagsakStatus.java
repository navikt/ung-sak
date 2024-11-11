package no.nav.ung.sak.fagsak.hendelse;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

public interface OppdaterFagsakStatus {
    void oppdaterFagsakNårBehandlingEndret(Behandling behandling);

    void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak);
}
