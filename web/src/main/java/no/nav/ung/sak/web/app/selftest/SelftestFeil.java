package no.nav.ung.sak.web.app.selftest;

import static no.nav.k9.felles.feil.LogLevel.ERROR;
import static no.nav.k9.felles.feil.LogLevel.WARN;

import java.io.IOException;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface SelftestFeil extends DeklarerteFeil {

    SelftestFeil FACTORY = FeilFactory.create(SelftestFeil.class);

    @TekniskFeil(feilkode = "K9-635121", feilmelding = "Klarte ikke å lese build time properties fil", logLevel = LogLevel.ERROR)
    Feil klarteIkkeÅLeseBuildTimePropertiesFil(IOException e);

    @TekniskFeil(feilkode = "K9-287026", feilmelding = "Dupliserte selftest navn %s", logLevel = WARN)
    Feil dupliserteSelftestNavn(String name);

    @TekniskFeil(feilkode = "K9-409676", feilmelding = "Uventet feil", logLevel = ERROR)
    Feil uventetSelftestFeil(IOException e);

    @TekniskFeil(feilkode = "K9-932415", feilmelding = "Selftest ERROR: %s. Endpoint: %s. Responstid: %s. Feilmelding: %s.", logLevel = ERROR)
    Feil kritiskSelftestFeilet(String description, String endpoint, String responseTime, String message);

    @TekniskFeil(feilkode = "984256", feilmelding = "Selftest ERROR: %s. Endpoint: %s. Responstid: %s. Feilmelding: %s.", logLevel = WARN)
    Feil ikkeKritiskSelftestFeilet(String description, String endpoint, String responsTime, String message);
}
