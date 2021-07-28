package no.nav.k9.sak.behandlingskontroll.impl;

import static no.nav.k9.felles.feil.LogLevel.ERROR;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface BehandlingskontrollFeil extends DeklarerteFeil {

    BehandlingskontrollFeil FACTORY = FeilFactory.create(BehandlingskontrollFeil.class);

    @TekniskFeil(feilkode = "FP-143308", feilmelding = "BehandlingId %s er allerede avsluttet, kan ikke henlegges", logLevel = ERROR)
    Feil kanIkkeHenleggeAvsluttetBehandling(Long behandlingId);

    @TekniskFeil(feilkode = "FP-105126", feilmelding = "BehandlingId %s har flere enn et aksjonspunkt, hvor aksjonspunktet fører til tilbakehopp ved gjenopptakelse. Kan ikke gjenopptas.", logLevel = ERROR)
    Feil kanIkkeGjenopptaBehandlingFantFlereAksjonspunkterSomMedførerTilbakehopp(Long behandlingId);


}
