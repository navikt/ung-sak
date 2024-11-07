package no.nav.k9.sak.behandlingslager.fagsak;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.sak.typer.Saksnummer;

interface FagsakFeil extends DeklarerteFeil {

    FagsakFeil FACTORY = FeilFactory.create(FagsakFeil.class);

    @TekniskFeil(feilkode = "FP-429883", feilmelding = "Det var flere enn en Fagsak for saksnummer: %s", logLevel = LogLevel.WARN)
    Feil flereEnnEnFagsakForSaksnummer(Saksnummer saksnummer);

}
