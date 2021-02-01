package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface SøknadUtbetalingOmsorgspengerFeil extends DeklarerteFeil {
    SøknadUtbetalingOmsorgspengerFeil FACTORY = FeilFactory.create(SøknadUtbetalingOmsorgspengerFeil.class);

    @TekniskFeil(feilkode = "FP-642746", feilmelding = "Feil i søknad om utbetaling av omsorgspenger: %s", logLevel = WARN, exceptionClass = DokumentValideringException.class)
    Feil valideringsfeilSøknadUtbetalingOmsorgspenger(String detaljer);

    @TekniskFeil(feilkode = "FP-544494", feilmelding = "Parsefeil i søknad om utbetaling av omsorgspenger", logLevel = WARN, exceptionClass = DokumentValideringException.class)
    Feil parsefeil(Exception cause);
}
