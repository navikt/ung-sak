package no.nav.k9.sak.web.app.tjenester.notat;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.jpa.TomtResultatException;

public interface NotatFeil extends DeklarerteFeil {
    NotatFeil FACTORY = FeilFactory.create(NotatFeil.class);

    @TekniskFeil(feilkode = "K9-NOTAT-100001", feilmelding = "Fant ikke sak", logLevel = LogLevel.ERROR, exceptionClass = TomtResultatException.class)
    Feil fantIkkeSak();

    @TekniskFeil(feilkode = "K9-NOTAT-100002", feilmelding = "Fant ikke notat", logLevel = LogLevel.ERROR, exceptionClass = TomtResultatException.class)
    Feil fantIkkeNotat();

    @TekniskFeil(feilkode = "K9-NOTAT-100003", feilmelding = "Klarte ikke å endre notat med versjon=%d da den er utdatert. Nyeste versjon=%d. Refresh og prøv igjen. Husk å ta vare på teksten din før refresh", logLevel = LogLevel.ERROR)
    Feil notatUtdatert(long versjon, long nyesteVersjon);

    @ManglerTilgangFeil(feilkode = "K9-NOTAT-100003", feilmelding = "Kun egne notater kan endres", logLevel = LogLevel.ERROR)
    Feil eierIkkeNotat();
}
