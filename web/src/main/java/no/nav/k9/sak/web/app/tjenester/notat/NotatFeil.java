package no.nav.k9.sak.web.app.tjenester.notat;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.jpa.TomtResultatException;

public interface NotatFeil extends DeklarerteFeil {
    NotatFeil FACTORY = FeilFactory.create(NotatFeil.class);

    @TekniskFeil(feilkode = "K9-NOTAT-100001", feilmelding = "Fant ikke sak", logLevel = LogLevel.ERROR, exceptionClass = TomtResultatException.class)
    Feil fantIkkeSak();

    @TekniskFeil(feilkode = "K9-NOTAT-100002", feilmelding = "Fant ikke notat", logLevel = LogLevel.ERROR, exceptionClass = TomtResultatException.class)
    Feil fantIkkeNotat();
}
