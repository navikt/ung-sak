package no.nav.foreldrepenger.økonomi.simulering.klient;

import java.util.Optional;

import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimulerOppdragDto;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;

public interface FpOppdragRestKlient {

    /**
     * Starter en simulering for gitt behandling med oppdrag XMLer.
     * @param request med behandlingId og liste med oppdrag-XMLer
     */
    void startSimulering(SimulerOppdragDto request);


    /**
     * Henter simuleringresultat for behandling hvis det finnes.
     * @param behandlingId
     * @return Optional med SimuleringResultatDto kan være tom
     */
    Optional<SimuleringResultatDto> hentResultat(Long behandlingId);

}
