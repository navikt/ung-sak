package no.nav.k9.sak.produksjonsstyring.arbeidsfordeling;

import static no.nav.vedtak.feil.LogLevel.WARN;

import java.util.List;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnAlleBehandlendeEnheterListeUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface ArbeidsfordelingFeil extends DeklarerteFeil {

    ArbeidsfordelingFeil FACTORY = FeilFactory.create(ArbeidsfordelingFeil.class);

    @TekniskFeil(feilkode = "FP-124143", feilmelding = "Ugyldig input til finn behandlende enhet", logLevel = LogLevel.ERROR)
    Feil finnBehandlendeEnhetListeUgyldigInput(FinnBehandlendeEnhetListeUgyldigInput e);

    @TekniskFeil(feilkode = "FP-569566", feilmelding = "Finner ikke behandlende enhet for geografisk tilknytning '%s', diskresjonskode '%s', ytelseType '%s'", logLevel = WARN)
    Feil finnerIkkeBehandlendeEnhet(String geografiskTilknytning, String diskresjonskode, FagsakYtelseType ytelseType);

    @TekniskFeil(feilkode = "FP-004703", feilmelding = "Forventet en, men fikk flere alternative behandlende enheter for geografisk tilknytning '%s', diskresjonskode '%s', ytelseType '%s': '%s'. Valgte '%s'", logLevel = WARN)
    Feil fikkFlereBehandlendeEnheter(String geografiskTilknytning, String diskresjonskode, FagsakYtelseType ytelseType, List<String> enheter, String valgtEnhet);

    @TekniskFeil(feilkode = "FP-678703", feilmelding = "Finner ikke alle behandlende enheter for ytelseType '%s'", logLevel = WARN)
    Feil finnerIkkeAlleBehandlendeEnheter(FagsakYtelseType ytelseType);

    @TekniskFeil(feilkode = "FP-324042", feilmelding = "Ugyldig input til finn alle behandlende enheter", logLevel = LogLevel.ERROR)
    Feil finnAlleBehandlendeEnheterListeUgyldigInput(FinnAlleBehandlendeEnheterListeUgyldigInput e);
}
