package no.nav.ung.sak.skjæringstidspunkt;

import no.nav.ung.sak.typer.Saksnummer;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface SkjæringstidspunktFeil extends DeklarerteFeil {

    SkjæringstidspunktFeil FACTORY = FeilFactory.create(SkjæringstidspunktFeil.class);

    @TekniskFeil(feilkode = "FP-312374", feilmelding = "Kan ikke finne %s fra søknad i Vedtaksløsningen selv om annen part har sak i VL for samme barn", logLevel = LogLevel.ERROR)
    Feil brukersSaknummerIkkeFunnetIVLSelvOmAnnenPartsSakErDer(Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-931232", feilmelding = "Finner ikke skjæringstidspunkt for foreldrepenger som forventet for behandling=%s", logLevel = LogLevel.WARN)
    Feil finnerIkkeSkjæringstidspunktForForeldrepenger(Long behandlingId);

    @TekniskFeil(feilkode = "FP-783491", feilmelding = "Kan ikke utlede opplysningsperiode for %s", logLevel = LogLevel.WARN)
    Feil kanIkkeUtledeOpplysningsperiodeForBehandling(Long behandlingId);
}
