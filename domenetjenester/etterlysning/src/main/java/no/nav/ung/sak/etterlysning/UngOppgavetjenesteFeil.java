package no.nav.ung.sak.etterlysning;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;

public interface UngOppgavetjenesteFeil extends DeklarerteFeil {

    UngOppgavetjenesteFeil FACTORY = FeilFactory.create(UngOppgavetjenesteFeil.class);

    @IntegrasjonFeil(feilkode = "UNG-100011", feilmelding = "Feil ved kall til oppgavetjenesten.", logLevel = LogLevel.ERROR)
    Feil feilVedKallTilUngOppgaveTjeneste(Exception e);
}
