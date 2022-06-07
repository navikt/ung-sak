package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;

@Dependent
public class SykdomProsessDriver {

    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private BehandlingModellRepository behandlingModellRepository;

    @Inject
    public SykdomProsessDriver(AksjonspunktKontrollRepository aksjonspunktKontrollRepository,
                               BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                               BehandlingModellRepository behandlingModellRepository) {
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingModellRepository = behandlingModellRepository;
    }

    /*
     * NB!
     * Må gjøres kompenserende tiltak for å ha kontroll på prossessen.
     * Dette bryter med det overordnede arkitektur ved å skrive til behandling utenfor
     * task/prosess-steg og aksjonsoppdaterer. Så må gjenopprette noe av sikkerheten aksjonspunkt rest gir oss.
     * Burde vært konvertert til aksjonspunkt som resten av løsningen og er ikke et eksempel til etterfølgelse.
     *
     * Her må det sikres at
     * - Behandlingen har aksjonspunktet
     * - Aksjonspunktet blir satt til opprettet og prosessen flyttet tilbake hvis passert vurderingspunkt
     */
    public void validerTilstand(Behandling behandling, boolean dryRun) {
        if (dryRun) {
            return;
        }

        var aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE);
        if (aksjonspunkt.isEmpty()) {
            // Legg til
            aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
        } else if (aksjonspunkt.get().erUtført() || aksjonspunkt.get().erAvbrutt()) {
            // Reåpner tidligere lukkede aksjonspunkt
            aksjonspunktKontrollRepository.setReåpnet(aksjonspunkt.get());
        }

        if (harPassertSykdom(behandling)) {
            // Flytter prosessen tilbake til sykdom
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(behandling, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR, null);
        }
    }

    private boolean harPassertSykdom(Behandling behandling) {
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return modell.erStegAFørStegB(BehandlingStegType.VURDER_MEDISINSKE_VILKÅR, steg);
    }
}
