package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunkt;

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
