package no.nav.k9.sak.mottak.inntektsmelding;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface MottattInntektsmeldingFeil extends DeklarerteFeil {

    MottattInntektsmeldingFeil FACTORY = FeilFactory.create(MottattInntektsmeldingFeil.class);

    @TekniskFeil(feilkode = "FP-947147", feilmelding = "Ukjent dokument %s", logLevel = WARN, exceptionClass = MottattInntektsmeldingException.class)
    Feil ukjentSkjemaType(String skjemaType);

    @TekniskFeil(feilkode = "FP-947148", feilmelding = "Mer enn en implementasjon funnet for skjematype %s", logLevel = WARN, exceptionClass = MottattInntektsmeldingException.class)
    Feil flereImplementasjonerAvSkjemaType(String skjemaType);

    @TekniskFeil(feilkode = "FP-187532", feilmelding = "Behandling [%s] pågår, avventer å håndtere mottatt dokument til det er prosessert", logLevel = WARN, exceptionClass = MottattInntektsmeldingException.class)
    Feil behandlingPågårAvventerKnytteMottattDokumentTilBehandling(Long id);

    @TekniskFeil(feilkode = "FP-187533", feilmelding = "Ugyldig inntektsmelding , bryter med forretningsregel validering: %s", logLevel = WARN, exceptionClass = MottattInntektsmeldingException.class)
    Feil inntektsmeldingSemantiskValideringFeil(String feilmelding);

    @TekniskFeil(feilkode = "FP-958724", feilmelding = "Fant ikke xsd for namespacet '%s'", logLevel = WARN, exceptionClass = MottattInntektsmeldingException.class)
    Feil ukjentNamespace(String namespace);

    @TekniskFeil(feilkode = "FP-312346", feilmelding = "Feil ved parsing av ukjent journaldokument-type med namespace '%s'", logLevel = ERROR, exceptionClass = MottattInntektsmeldingException.class)
    Feil uventetFeilVedParsingAvXml(String namespace, Exception e);
}
