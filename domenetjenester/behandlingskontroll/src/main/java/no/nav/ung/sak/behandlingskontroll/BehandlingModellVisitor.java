package no.nav.ung.sak.behandlingskontroll;

/**
 * Visitor som kan benyttes til å traversere en sekvens av {@link BehandlingSteg}.
 *
 */
public interface BehandlingModellVisitor {

    /**
     * Kall på et {@link BehandlingSteg}.
     *
     * @param steg - modell av steg som skal kalles
     * @return {@link StegProsesseringResultat}
     */
    StegProsesseringResultat prosesser(BehandlingStegModell steg);

}
