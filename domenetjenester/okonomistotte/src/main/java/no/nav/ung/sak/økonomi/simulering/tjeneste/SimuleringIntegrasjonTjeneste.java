package no.nav.ung.sak.økonomi.simulering.tjeneste;

import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

import java.util.Optional;

public interface SimuleringIntegrasjonTjeneste {
    void startSimulering(Behandling behandling);

    void kansellerSimulering(Behandling behandling);

    Optional<SimuleringResultatDto> hentResultat(Behandling behandling);
}
