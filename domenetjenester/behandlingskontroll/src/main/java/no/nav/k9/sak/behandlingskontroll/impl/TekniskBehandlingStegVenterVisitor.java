package no.nav.k9.sak.behandlingskontroll.impl;

import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.StegProsesseringResultat;
import no.nav.k9.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;

/**
 * Tekniske oppsett ved kjøring av et steg:<br>
 * <ul>
 * <li>Setter savepoint slik at dersom steg feiler så beholdes tidligere resultater.</li>
 * <li>Setter LOG_CONTEXT slik at ytterligere detaljer blir med i logging.</li>
 * </ul>
 */
public class TekniskBehandlingStegVenterVisitor extends TekniskBehandlingStegVisitor {

    private boolean gjenoppta = true;

    public TekniskBehandlingStegVenterVisitor(BehandlingskontrollServiceProvider repositoryProvider,
                                              BehandlingskontrollKontekst kontekst) {
        super(repositoryProvider, kontekst);
    }


    @Override
    protected StegProsesseringResultat prosesserSteg(BehandlingStegVisitor stegVisitor) {
        StegProsesseringResultat resultat = gjenoppta ? stegVisitor.gjenoppta() : stegVisitor.prosesser();
        gjenoppta = false;
        return resultat;
    }
}
