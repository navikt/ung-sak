package no.nav.k9.sak.mottak.dokumentmottak;

public class MottattDokumentException extends DokumentValideringException {

    public static final Factory FACTORY = new Factory();

    public MottattDokumentException(String feilmelding) {
        super(feilmelding);
    }

    public MottattDokumentException(String feilmelding, Exception cause) {
        super(feilmelding, cause);
    }


    public static class Factory {
        public MottattDokumentException ukjentSkjemaType(String skjemaType) {
            return new MottattDokumentException("Ukjent dokument " + skjemaType);
        }

        public MottattDokumentException flereImplementasjonerAvSkjemaType(String skjemaType) {
            return new MottattDokumentException("Mer enn en implementasjon funnet for skjematype " + skjemaType);
        }

        public MottattDokumentException behandlingPågårAvventerKnytteMottattDokumentTilBehandling(Long id) {
            return new MottattDokumentException("Behandling [" + id + "] pågår, avventer å håndtere mottatt dokument til det er prosessert");
        }

        public MottattDokumentException behandlingUnderIverksettingAvventerKnytteMottattDokumentTilBehandling(Long id) {
            return new MottattDokumentException("Behandling [" + id + "] er i iverksetting-status og kan ikke hoppes tilbake. Avventer å håndtere mottatt dokument til behandlingen er avsluttet");
        }

        public MottattDokumentException inntektsmeldingSemantiskValideringFeil(String feilmelding) {
            return new MottattDokumentException("Ugyldig inntektsmelding , bryter med forretningsregel validering: " + feilmelding);
        }

        public MottattDokumentException ukjentNamespace(String namespace) {
            return new MottattDokumentException("Fant ikke xsd for namespacet '" + namespace + "'");
        }

        public MottattDokumentException uventetFeilVedParsingAvXml(String namespace, Exception e) {
            return new MottattDokumentException("Feil ved parsing av ukjent journaldokument-type med namespace '" + namespace + "'", e);
        }
    }

}
