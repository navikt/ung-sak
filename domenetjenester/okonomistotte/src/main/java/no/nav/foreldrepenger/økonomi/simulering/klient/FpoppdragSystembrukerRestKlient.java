package no.nav.foreldrepenger.økonomi.simulering.klient;

public interface FpoppdragSystembrukerRestKlient {

    /**
     * Kansellerer aktivt simuleringresultat for behandling, hvis det finnes.
     *
     * @param behandlingId
     */
    void kansellerSimulering(Long behandlingId);

}
