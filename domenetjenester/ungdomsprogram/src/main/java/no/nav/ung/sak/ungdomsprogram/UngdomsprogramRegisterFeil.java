package no.nav.ung.sak.ungdomsprogram;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;

public interface UngdomsprogramRegisterFeil extends DeklarerteFeil {

    UngdomsprogramRegisterFeil FACTORY = FeilFactory.create(UngdomsprogramRegisterFeil.class);

    @IntegrasjonFeil(feilkode = "UNG-100001", feilmelding = "Feil ved kall til ungdomsregistertjenesten.", logLevel = LogLevel.ERROR)
    Feil feilVedKallTilUngRegister(Exception e);
}
