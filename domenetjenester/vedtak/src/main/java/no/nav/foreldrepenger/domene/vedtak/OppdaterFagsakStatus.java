package no.nav.foreldrepenger.domene.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public interface OppdaterFagsakStatus {
    void oppdaterFagsakNårBehandlingEndret(Behandling behandling);

    void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak);
}
