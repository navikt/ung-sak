package no.nav.k9.sak.web.app.oppgave;

import static no.nav.k9.felles.feil.LogLevel.ERROR;
import static no.nav.k9.felles.feil.LogLevel.WARN;

import java.io.UnsupportedEncodingException;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

interface OppgaveRedirectServletFeil extends DeklarerteFeil {

    OppgaveRedirectServletFeil FACTORY = FeilFactory.create(OppgaveRedirectServletFeil.class);

    @TekniskFeil(feilkode = "FP-022644", feilmelding = "Sak kan ikke åpnes, da referanse mangler.", logLevel = WARN)
    Feil sakKanIkkeÅpnesDaReferanseMangler();

    @TekniskFeil(feilkode = "FP-026576", feilmelding = "Det finnes ingen sak med dette saksnummeret: %s", logLevel = WARN)
    Feil detFinnesIngenFagsak(String saksnummer);

    @TekniskFeil(feilkode = "FP-243576", feilmelding = "Det finnes ingen oppgave med denne referansen: %s", logLevel = WARN)
    Feil detFinnesIngenOppgaveMedDenneReferansen(String oppgaveId);

    @TekniskFeil(feilkode = "FP-196534", feilmelding = "Oppgaven med %s er ikke registrert på sak %s", logLevel = ERROR)
    Feil oppgaveErIkkeRegistrertPåSak(String oppgaveId, String saksnummer);

    @TekniskFeil(feilkode = "FP-616439", feilmelding = "Kunne ikke encode feilmelding %s", logLevel = ERROR)
    Feil kunneIkkeEncodeFeilmelding(String feilmelding, UnsupportedEncodingException exception);

}

