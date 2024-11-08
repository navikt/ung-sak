package no.nav.k9.sak.behandlingslager.behandling;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

interface BehandlingFeil extends DeklarerteFeil {

    @TekniskFeil(feilkode = "FP-918665", feilmelding = "Ugyldig antall behandlingsresultat, forventer maks 1 per behandling, men har %s", logLevel = LogLevel.WARN)
    Feil merEnnEttBehandlingsresultat(Integer antall);

    @TekniskFeil(feilkode = "FP-138032", feilmelding = "Behandling har ikke aksjonspunkt for definisjon [%s].", logLevel = LogLevel.ERROR)
    Feil aksjonspunktIkkeFunnet(String kode);

}
