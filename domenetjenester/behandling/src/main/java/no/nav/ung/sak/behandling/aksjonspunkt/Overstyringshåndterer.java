package no.nav.ung.sak.behandling.aksjonspunkt;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunkt;

public interface Overstyringshåndterer<T extends OverstyringAksjonspunkt> {

    OppdateringResultat håndterOverstyring(T dto, Behandling behandling, BehandlingskontrollKontekst kontekst);

    /**
     * Opprett Aksjonspunkt for Overstyring og håndter lagre historikk.
     */
    void håndterAksjonspunktForOverstyringPrecondition(T dto, Behandling behandling);

    /**
     * Opprett Aksjonspunkt for Overstyring og håndter lagre historikk.
     */
    void håndterAksjonspunktForOverstyringHistorikk(T dto, Behandling behandling, boolean endretBegrunnelse);

    AksjonspunktDefinisjon aksjonspunktForInstans();
}
