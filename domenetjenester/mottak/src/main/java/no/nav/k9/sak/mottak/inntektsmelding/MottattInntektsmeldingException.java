package no.nav.k9.sak.mottak.inntektsmelding;

import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;

public class MottattInntektsmeldingException extends DokumentValideringException {

    public MottattInntektsmeldingException(String feilmelding) {
        super(feilmelding);
    }

    public MottattInntektsmeldingException(String feilmelding, Exception cause) {
        super(feilmelding, cause);
    }

    public static MottattInntektsmeldingException ukjentSkjemaType(String skjemaType) {
        return new MottattInntektsmeldingException("Ukjent dokument " + skjemaType);
    }

    public static MottattInntektsmeldingException flereImplementasjonerAvSkjemaType(String skjemaType) {
        return new MottattInntektsmeldingException("Mer enn en implementasjon funnet for skjematype " + skjemaType);
    }

    public static MottattInntektsmeldingException inntektsmeldingSemantiskValideringFeil(String feilmelding) {
        return new MottattInntektsmeldingException("Ugyldig inntektsmelding , bryter med forretningsregel validering: " + feilmelding);
    }

    public static MottattInntektsmeldingException ukjentNamespace(String namespace) {
        return new MottattInntektsmeldingException("Fant ikke xsd for namespacet '" + namespace + "'");
    }

    public static MottattInntektsmeldingException uventetFeilVedParsingAvXml(String namespace, Exception e) {
        return new MottattInntektsmeldingException("Feil ved parsing av ukjent journaldokument-type med namespace '" + namespace + "'", e);
    }
}
