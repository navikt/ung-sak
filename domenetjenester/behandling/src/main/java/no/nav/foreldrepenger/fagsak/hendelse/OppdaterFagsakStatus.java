package no.nav.foreldrepenger.fagsak.hendelse;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public interface OppdaterFagsakStatus {
    void oppdaterFagsakNårBehandlingEndret(Behandling behandling);

    void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak);
}
