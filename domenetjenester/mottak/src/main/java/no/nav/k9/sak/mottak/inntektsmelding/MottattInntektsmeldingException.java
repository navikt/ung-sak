package no.nav.k9.sak.mottak.inntektsmelding;

import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;

public class MottattInntektsmeldingException extends DokumentValideringException {

    public static final Factory FACTORY = new Factory();

    public MottattInntektsmeldingException(String feilmelding) {
        super(feilmelding);
    }

    public MottattInntektsmeldingException(String feilmelding, Exception cause) {
        super(feilmelding, cause);
    }


    public static class Factory {
        public MottattInntektsmeldingException ukjentSkjemaType(String skjemaType) {
            return new MottattInntektsmeldingException("Ukjent dokument " + skjemaType);
        }

        public MottattInntektsmeldingException flereImplementasjonerAvSkjemaType(String skjemaType) {
            return new MottattInntektsmeldingException("Mer enn en implementasjon funnet for skjematype " + skjemaType);
        }

        public MottattInntektsmeldingException behandlingPågårAvventerKnytteMottattDokumentTilBehandling(Long id) {
            return new MottattInntektsmeldingException("Behandling [" + id + "] pågår, avventer å håndtere mottatt dokument til det er prosessert");
        }

        public MottattInntektsmeldingException inntektsmeldingSemantiskValideringFeil(String feilmelding) {
            return new MottattInntektsmeldingException("Ugyldig inntektsmelding , bryter med forretningsregel validering: " + feilmelding);
        }

        public MottattInntektsmeldingException ukjentNamespace(String namespace) {
            return new MottattInntektsmeldingException("Fant ikke xsd for namespacet '" + namespace + "'");
        }

        public MottattInntektsmeldingException uventetFeilVedParsingAvXml(String namespace, Exception e) {
            return new MottattInntektsmeldingException("Feil ved parsing av ukjent journaldokument-type med namespace '" + namespace + "'", e);
        }
    }

}
