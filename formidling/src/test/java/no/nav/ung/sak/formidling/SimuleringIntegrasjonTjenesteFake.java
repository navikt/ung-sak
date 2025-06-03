package no.nav.ung.sak.formidling;

import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimuleringIntegrasjonTjenesteFake implements SimuleringIntegrasjonTjeneste {
    private final Map<Long, SimuleringResultatDto> resultater = new HashMap<>();

    public SimuleringIntegrasjonTjenesteFake() {
    }

    @Override
    public void startSimulering(Behandling behandling) {
        throw new UnsupportedOperationException("Ikke implementert");
    }

    @Override
    public void kansellerSimulering(Behandling behandling) {
        throw new UnsupportedOperationException("Ikke implementert");
    }

    @Override
    public Optional<SimuleringResultatDto> hentResultat(Behandling behandling) {
        return Optional.ofNullable(resultater.get(behandling.getId()));
    }

    public void leggTilResultat(Behandling behandling, SimuleringResultatDto resultat) {
        resultater.put(behandling.getId(), resultat);
    }
}
